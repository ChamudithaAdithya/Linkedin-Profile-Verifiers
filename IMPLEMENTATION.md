# SIMLEA — Implementation Plan

**Project:** Multi-Source Professional Profile Enrichment Platform  
**Stack:** Angular 21 + Spring Boot 4.1 / Java 25 + PostgreSQL  
**Inspiration:** SnapADDY-style identity resolution (not a LinkedIn scraper)

---

## Architecture Overview

```
Angular 21 (Standalone)
    │
    │ HTTP (localhost:4200 → localhost:8080)
    ▼
Spring Boot 4.1 API Gateway
    │
    ├── Discovery Layer
    │       ├── Google Custom Search JSON API
    │       ├── Bing Web Search API v7
    │       ├── GitHub Search API
    │       └── Company website fetcher (Jsoup / HttpClient)
    │
    ├── Entity Extraction Layer
    │       └── Parse names, titles, companies, URLs from snippets + pages
    │
    ├── Entity Matching Engine (core IP)
    │       └── Rule-based scoring → merge candidates → resolved profile
    │
    ├── Enrichment & Caching Layer
    │       └── Lookup details, confidence scoring, store in PostgreSQL
    │
    └── REST Controllers
            ├── GET  /api/enrich/search?q=...
            ├── GET  /api/enrich/profile?id=...
            └── POST /api/enrich/refresh (re-enrich existing profile)
```

---

## Phase 1 — Database & Backend Foundation

### 1.1 Add Dependencies to `pom.xml`

```xml
<!-- JPA + PostgreSQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- HTML parsing for company websites -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>

<!-- Caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 1.2 Database Schema (PostgreSQL)

```sql
CREATE TABLE raw_search_result (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query       VARCHAR(500) NOT NULL,
    source      VARCHAR(50) NOT NULL,     -- 'google', 'bing', 'github', 'website'
    title       TEXT,
    snippet     TEXT,
    url         TEXT NOT NULL,
    fetched_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE source_profile (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source      VARCHAR(50) NOT NULL,
    full_name   VARCHAR(255),
    headline    VARCHAR(500),
    company     VARCHAR(255),
    location    VARCHAR(255),
    profile_url VARCHAR(500),
    photo_url   VARCHAR(500),
    raw_json    JSONB,
    fetched_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE resolved_profile (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name     VARCHAR(255),
    headline      VARCHAR(500),
    company       VARCHAR(255),
    location      VARCHAR(255),
    linkedin_url  VARCHAR(500),
    github_url    VARCHAR(500),
    website_url   VARCHAR(500),
    email         VARCHAR(255),
    phone         VARCHAR(50),
    photo_url     VARCHAR(500),
    confidence    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    source_ids    UUID[],
    raw_data      JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_raw_query ON raw_search_result(query);
CREATE INDEX idx_resolved_name ON resolved_profile(full_name);
CREATE INDEX idx_resolved_company ON resolved_profile(company);
```

### 1.3 `application.properties` Additions

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/simlea
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Google Custom Search API
search.google.api-key=${GOOGLE_API_KEY}
search.google.cx=${GOOGLE_CX}

# Bing Search API (optional)
search.bing.api-key=${BING_API_KEY}

# Caching
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=1h
```

### 1.4 New Java Package Structure

```
com.example.SimleaBackendTest/
├── entity/
│   ├── RawSearchResult.java
│   ├── SourceProfile.java
│   └── ResolvedProfile.java
├── repository/
│   ├── RawSearchResultRepository.java
│   ├── SourceProfileRepository.java
│   └── ResolvedProfileRepository.java
├── service/
│   ├── discovery/
│   │   ├── DiscoveryService.java
│   │   ├── GoogleSourceConnector.java
│   │   ├── BingSourceConnector.java
│   │   └── WebsiteConnector.java
│   ├── extraction/
│   │   └── EntityExtractor.java
│   ├── matching/
│   │   ├── EntityMatchingEngine.java
│   │   └── ScoringRule.java
│   └── enrichment/
│       └── EnrichmentService.java
├── controller/
│   ├── LinkedInController.java  (existing)
│   └── EnrichController.java    (new)
└── config/
    ├── WebConfig.java           (existing)
    ├── LinkedInProperties.java  (existing)
    └── SearchProperties.java    (new)
```

---

## Phase 2 — Discovery Layer

### 2.1 `SearchProperties.java`

Type-safe binding for search API keys/URLs:

```java
@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    private Google google = new Google();
    private Bing bing = new Bing();

    // Nested static classes with getters/setters
    public static class Google {
        private String apiKey;
        private String cx;
        // ...
    }
    public static class Bing {
        private String apiKey;
        // ...
    }
}
```

### 2.2 Source Connectors

Each connector implements this interface:

```java
public interface SourceConnector {
    String sourceName();
    List<RawSearchResult> search(String query);
}
```

#### `GoogleSourceConnector.java`

- Calls `https://www.googleapis.com/customsearch/v1?key=...&cx=...&q=...`
- Parses `items[].title`, `items[].snippet`, `items[].link`
- Returns list of `RawSearchResult` with `source = "google"`

#### `BingSourceConnector.java`

- Calls `https://api.bing.microsoft.com/v7.0/search?q=...`
- Header: `Ocp-Apim-Subscription-Key`
- Returns list of `RawSearchResult` with `source = "bing"`

#### `WebsiteConnector.java`

- Extracts metadata from company/personal pages discovered in search results
- Uses Jsoup to fetch and parse `<title>`, `<meta name="description">`, Open Graph tags
- Returns list of `RawSearchResult` with `source = "website"`

### 2.3 `DiscoveryService.java`

Orchestrates all connectors:

```java
@Service
public class DiscoveryService {
    private final List<SourceConnector> connectors;
    private final RawSearchResultRepository rawRepo;

    public List<RawSearchResult> discover(String query) {
        List<RawSearchResult> all = new ArrayList<>();
        for (var connector : connectors) {
            all.addAll(connector.search(query));
        }
        rawRepo.saveAll(all);
        return all;
    }
}
```

### 2.4 Query Strategy

When user searches `"John Smith Google"`, transform into multiple targeted queries:

| Query | Target |
|-------|--------|
| `"John Smith" Google` | exact name + company |
| `site:linkedin.com/in "John Smith" Google` | LinkedIn profile |
| `"John Smith" software engineer` | generic search |
| `"John Smith" github` | GitHub profile |

The `DiscoveryService` sends all variants in parallel via virtual threads (Java 25):

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = queries.stream()
        .map(q -> executor.submit(() -> connector.search(q)))
        .toList();
    for (var f : futures) { all.addAll(f.get()); }
}
```

---

## Phase 3 — Entity Extraction

### 3.1 `EntityExtractor.java`

Parses raw search results into structured `SourceProfile` objects.

**Extraction rules:**

| Field | Source |
|-------|--------|
| `full_name` | Title text before the pipe or first ` - ` separator. Fallback: regex for `[A-Z][a-z]+ [A-Z][a-z]+` |
| `headline` | Snippet up to first period. Remove common prefixes (`"LinkedIn: "`, `"Profile | "`) |
| `company` | After ` at ` or ` - ` in snippet. Also from `site:linkedin.com/in ... company name` |
| `location` | Regex for city, state patterns. From snippet context |
| `profile_url` | The URL itself. Normalize to canonical LinkedIn/GitHub URL form |
| `photo_url` | From Open Graph `og:image` meta tag (fetched via WebsiteConnector) |

### 3.2 `SourceProfileRepository`

Stores extracted profiles per source, keyed by source name + profile URL (to deduplicate within a source).

---

## Phase 4 — Entity Matching Engine (Core IP)

### 4.1 `EntityMatchingEngine.java`

Determines whether multiple `SourceProfile` records refer to the same person.

**Scoring system:**

| Rule | Weight | Condition |
|------|--------|-----------|
| Name exact match | +40 | `a.fullName.equalsIgnoreCase(b.fullName)` |
| Name fuzzy match | +25 | Levenshtein distance ≤ 2 |
| Company match | +20 | Same normalized company name |
| Location match | +10 | Same city/state |
| Title overlap | +15 | ≥2 significant words match ("Senior Software Engineer" → "Software Engineer") |
| URL overlap | +10 | Same LinkedIn URL in both |
| Email match | +30 | Same email (automatic merge) |

**Decision:** if `score ≥ 70` → same entity; if `50–69` → probable (flag for manual review); below → different.

### 4.2 Matching Pipeline

```
Search results (multiple sources)
       │
       ▼
EntityExtractor → List<SourceProfile>
       │
       ▼
EntityMatchingEngine
       │
       ├── Group by name similarity (blocking step)
       │   └── For each pair in block: compute score
       │       └── If score ≥ threshold: merge
       │
       ▼
ResolvedProfile + List of merged source IDs
       │
       ▼
Save to resolved_profile table
```

### 4.3 Java Implementation Sketch

```java
@Service
public class EntityMatchingEngine {
    private final List<ScoringRule> rules;

    public EntityMatchingEngine() {
        this.rules = List.of(
            new NameExactRule(40),
            new NameFuzzyRule(25),
            new CompanyMatchRule(20),
            new LocationMatchRule(10),
            new TitleOverlapRule(15),
            new UrlOverlapRule(10),
            new EmailMatchRule(30)
        );
    }

    public MergeDecision evaluate(SourceProfile a, SourceProfile b) {
        int score = rules.stream().mapToInt(r -> r.score(a, b)).sum();
        if (score >= 70) return MergeDecision.MERGE;
        if (score >= 50) return MergeDecision.FLAG;
        return MergeDecision.SKIP;
    }

    public ResolvedProfile merge(List<SourceProfile> sources) {
        // Union of all fields (most confident wins),
        // aggregate source_ids, build raw_data JSON
    }
}
```

---

## Phase 5 — Enrichment & Caching

### 5.1 `EnrichmentService.java`

```
enrich(ResolvedProfile)
    │
    ├── If already cached and fresh (< 1h): return cached
    ├── Else:
    │       ├── Fetch LinkedIn og:image (via WebsiteConnector on profile_url)
    │       ├── Fetch GitHub profile bio/avatar (GitHub API)
    │       │    (public endpoints, no auth needed for basic info)
    │       ├── Re-score confidence based on available fields
    │       └── Update resolved_profile
    └── Return enriched profile
```

### 5.2 Caching

- `@Cacheable("profiles")` on `getProfileById()`
- Cache eviction on explicit refresh (`POST /api/enrich/refresh`)

---

## Phase 6 — REST Controllers

### 6.1 `EnrichController.java`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/enrich/search?q={query}` | Full pipeline: discover → extract → match → enrich → return resolved profiles |
| `GET` | `/api/enrich/profile/{id}` | Return cached resolved profile by ID |
| `POST` | `/api/enrich/refresh/{id}` | Force re-enrichment of an existing profile |
| `GET` | `/api/enrich/sources/{id}` | Return raw source profiles that fed into a resolved profile |

**Response format:**

```json
{
  "results": [
    {
      "id": "uuid",
      "fullName": "John Smith",
      "headline": "Senior Software Engineer",
      "company": "Google",
      "location": "Mountain View, CA",
      "linkedinUrl": "https://linkedin.com/in/john-smith",
      "githubUrl": "https://github.com/johnsmith",
      "photoUrl": "https://...",
      "confidence": 0.82,
      "sources": ["google", "github", "linkedin"]
    }
  ],
  "total": 1,
  "query": "John Smith Google"
}
```

---

## Phase 7 — Angular Dashboard UI

### 7.1 New files

```
simlea-frontend/src/app/
├── services/
│   └── enrich.service.ts           (new — search + profile API calls)
└── dashboard/
    ├── dashboard.ts                 (component)
    ├── dashboard.html               (template)
    └── dashboard.css                (styles)
```

### 7.2 `enrich.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ResolvedProfile {
  id: string;
  fullName: string;
  headline: string;
  company: string;
  location: string;
  linkedinUrl: string;
  githubUrl: string;
  websiteUrl: string;
  photoUrl: string;
  email: string;
  confidence: number;
}

export interface SearchResponse {
  results: ResolvedProfile[];
  total: number;
  query: string;
}

@Injectable({ providedIn: 'root' })
export class EnrichService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/enrich';

  search(query: string): Observable<SearchResponse> {
    return this.http.get<SearchResponse>(`${this.baseUrl}/search`, {
      params: { q: query }
    });
  }

  getProfile(id: string): Observable<ResolvedProfile> {
    return this.http.get<ResolvedProfile>(`${this.baseUrl}/profile/${id}`);
  }

  refreshProfile(id: string): Observable<ResolvedProfile> {
    return this.http.post<ResolvedProfile>(`${this.baseUrl}/refresh/${id}`, {});
  }
}
```

### 7.3 Dashboard Component Layout

```
┌─────────────────────────────────────────────────────┐
│  SIMLEA Professionals Finder                         │
│  ┌─────────────────────────────────────────────┐    │
│  │ [Search input...                   ] [Search]│    │
│  └─────────────────────────────────────────────┘    │
├────────────────────────┬────────────────────────────┤
│  Results Pane (35%)    │  Detail Pane (65%)          │
│                        │                             │
│  ┌──────────────────┐  │  ┌─────────────────────┐   │
│  │ 👤 John Smith    │  │  │  👤                  │   │
│  │ Google           │  │  │  John Smith          │   │
│  │ Software Eng     │  │  │  Senior Software Eng │   │
│  │ Mountain View    │  │  │  Google              │   │
│  └──────────────────┘  │  │  Mountain View, CA    │   │
│  ┌──────────────────┐  │  │                      │   │
│  │ 👤 Jane Doe      │  │  │  🔗 LinkedIn         │   │
│  │ Microsoft        │  │  │  🔗 GitHub           │   │
│  │ Product Manager  │  │  │  🔗 Company Website  │   │
│  │ Redmond, WA      │  │  │                      │   │
│  └──────────────────┘  │  │  📧 j.smith@...      │   │
│                        │  │                      │   │
│                        │  │  Confidence: 82%     │   │
│                        │  │  [████████░░]        │   │
│                        │  │                      │   │
│                        │  │  Sources: Google +   │   │
│                        │  │  GitHub + LinkedIn   │   │
│                        │  └─────────────────────┘   │
└────────────────────────┴────────────────────────────┘
```

### 7.4 Route Update (`app.routes.ts`)

```typescript
export const routes: Routes = [
  { path: '', component: Login },
  { path: 'auth/callback', component: Callback },
  { path: 'dashboard', component: Dashboard },
];
```

Navigation after successful login → redirect to `/dashboard`.

---

## Phase 8 — Integration & Testing

### 8.1 Test Scenarios

| Scenario | Input | Expected |
|----------|-------|----------|
| Basic search | `Jane Doe Microsoft` | Returns 1+ resolved profiles with confidence > 0 |
| Multi-source merge | `John Smith` (returns LinkedIn + GitHub results) | Single resolved profile with both sources linked |
| Empty results | `asdfghjkl12345` | Empty results array, HTTP 200 |
| Error handling | No API key configured | HTTP 502, descriptive error message |
| Cache hit | Same query twice | Second request < 50ms (not 1-3s) |

### 8.2 Unit Tests (Backend)

- `EntityMatchingEngineTest` — verify scoring against known pairs
- `EntityExtractorTest` — parse real-looking snippets
- Each `SourceConnector` — mock HTTP responses

### 8.3 Component Tests (Frontend)

- `Dashboard` — mock `EnrichService`, verify results render
- `EnrichService` — `HttpClient` mocking, verify URL construction

---

## MVP Roadmap (2–4 Weeks)

| Week | Deliverables |
|------|-------------|
| **Week 1** | PostgreSQL schema, JPA entities, repositories, Google source connector, raw search results storing |
| **Week 2** | Entity extractor, entity matching engine, basic merge pipeline, first end-to-end search → profile |
| **Week 3** | `EnrichController`, Angular dashboard component, search UI, profile detail view |
| **Week 4** | Caching, confidence scoring, error handling, manual verification UI, polish |

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Raw results → extracted profiles → resolved profiles** (3-table pipeline) | Each stage is independently debuggable and re-runnable |
| **Rule-based scoring first, AI later** | Simple to implement, explainable, no GPU/dependency needed |
| **PostgreSQL JSONB for raw_data** | Schema-flexible storage for heterogeneous source data |
| **Java 25 virtual threads** | Parallel search across sources with minimal overhead |
| **Caffeine cache** | Lightweight, production-grade, Spring Boot native |
| **Angular signals** | Fine-grained reactivity for search results list + profile detail |

---

## What We Are NOT Doing

- ❌ Scraping LinkedIn directly
- ❌ Storing LinkedIn profile data beyond publicly-available URLs
- ❌ Building a "search engine" — we discover and resolve, not index
- ❌ Real-time enrichment — it's a search-and-cache model
- ❌ AI/ML in v1 — starts with deterministic rules

---

## Summary

```
User types: "John Smith Google"
       │
       ▼
Angular → GET /api/enrich/search?q=John+Smith+Google
       │
       ▼
Spring Boot DiscoveryService
       ├── GoogleCustomSearch → 10 results
       ├── BingWebSearch      → 10 results
       └── (parallel via virtual threads)
       │
       ▼
EntityExtractor → List<SourceProfile>
       │
       ▼
EntityMatchingEngine → 1 ResolvedProfile (confidence: 0.82)
       │
       ▼
EnrichmentService
       ├── Fetch og:image from LinkedIn URL
       ├── Fetch GitHub profile
       └── Re-score confidence
       │
       ▼
Save to DB + Return JSON to Angular
       │
       ▼
Angular renders profile card with photo, links, confidence bar
```
