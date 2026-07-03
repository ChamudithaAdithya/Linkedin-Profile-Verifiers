package com.example.SimleaBackendTest.service.enrichment;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import com.example.SimleaBackendTest.entity.ResolvedProfile;
import com.example.SimleaBackendTest.entity.SourceProfile;
import com.example.SimleaBackendTest.repository.ResolvedProfileRepository;
import com.example.SimleaBackendTest.service.discovery.DiscoveryService;
import com.example.SimleaBackendTest.service.discovery.WebsiteConnector;
import com.example.SimleaBackendTest.service.extraction.EntityExtractor;
import com.example.SimleaBackendTest.service.matching.EntityMatchingEngine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class EnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentService.class);

    private final DiscoveryService discoveryService;
    private final EntityExtractor entityExtractor;
    private final EntityMatchingEngine matchingEngine;
    private final ResolvedProfileRepository resolvedProfileRepository;
    private final WebsiteConnector websiteConnector;

    public EnrichmentService(
            DiscoveryService discoveryService,
            EntityExtractor entityExtractor,
            EntityMatchingEngine matchingEngine,
            ResolvedProfileRepository resolvedProfileRepository,
            WebsiteConnector websiteConnector) {
        this.discoveryService = discoveryService;
        this.entityExtractor = entityExtractor;
        this.matchingEngine = matchingEngine;
        this.resolvedProfileRepository = resolvedProfileRepository;
        this.websiteConnector = websiteConnector;
    }

    public List<ResolvedProfile> searchAndEnrich(String query) {
        log.info("Starting enrichment pipeline for query: '{}'", query);
        List<RawSearchResult> rawResults = discoveryService.discover(query);
        return enrich(rawResults, query);
    }

    public List<ResolvedProfile> searchAndEnrichStructured(String name, String company, String location) {
        log.info("Starting structured enrichment: name='{}', company='{}', location='{}'",
                name, company, location);
        List<RawSearchResult> rawResults = discoveryService.discoverStructured(name, company, location);
        String label = (name != null ? name : "") + " " + (company != null ? company : "") + " " + (location != null ? location : "");
        return enrich(rawResults, label.trim());
    }

    private List<ResolvedProfile> enrich(List<RawSearchResult> rawResults, String label) {
        if (rawResults.isEmpty()) {
            log.info("No raw results found for: '{}'", label);
            return List.of();
        }

        List<RawSearchResult> pageResults = websiteConnector.enrichWithPageMetadata(rawResults, label);
        rawResults.addAll(pageResults);

        List<SourceProfile> extracted = entityExtractor.extract(rawResults);
        if (extracted.isEmpty()) {
            log.info("No profiles could be extracted for: '{}'", label);
            return List.of();
        }

        List<ResolvedProfile> resolved = matchingEngine.resolve(extracted);
        List<ResolvedProfile> saved = resolvedProfileRepository.saveAll(resolved);

        log.info("Enrichment complete for '{}': {} raw → {} extracted → {} resolved",
                label, rawResults.size(), extracted.size(), saved.size());

        return saved;
    }

    @Cacheable(value = "profiles", key = "#id")
    public Optional<ResolvedProfile> getProfileById(UUID id) {
        return resolvedProfileRepository.findById(id);
    }

    @CacheEvict(value = "profiles", key = "#id")
    public Optional<ResolvedProfile> refreshProfile(UUID id) {
        log.info("Refreshing profile: {}", id);
        return resolvedProfileRepository.findById(id).map(profile -> {
            profile.setConfidence(recalculateConfidence(profile));
            profile.getUpdatedAt();
            return resolvedProfileRepository.save(profile);
        });
    }

    private double recalculateConfidence(ResolvedProfile profile) {
        double score = 0.0;
        int checks = 0;

        if (profile.getFullName() != null && profile.getFullName().split("\\s+").length >= 2) {
            score += 20;
        }
        checks += 20;

        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            score += 20;
        }
        checks += 20;

        if (profile.getCompany() != null && !profile.getCompany().isBlank()) {
            score += 20;
        }
        checks += 20;

        if (profile.getLinkedinUrl() != null) {
            score += 15;
        }
        checks += 15;

        if (profile.getPhotoUrl() != null) {
            score += 10;
        }
        checks += 10;

        if (profile.getEmail() != null) {
            score += 15;
        }
        checks += 15;

        return checks > 0 ? (score / checks) * 100.0 : 0.0;
    }
}
