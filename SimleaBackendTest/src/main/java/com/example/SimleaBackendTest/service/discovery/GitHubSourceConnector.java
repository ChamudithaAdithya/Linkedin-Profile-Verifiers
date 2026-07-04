package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(GitHubSourceConnector.class);
    private static final String SEARCH_URL = "https://api.github.com/search/users";
    private static final String USER_URL = "https://api.github.com/users";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubSourceConnector() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "github";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        return searchStructured(query, null, null);
    }

    @Override
    public List<RawSearchResult> searchStructured(String name, String company, String location) {
        List<RawSearchResult> results = new ArrayList<>();

        try {
            StringBuilder q = new StringBuilder();
            if (name != null && !name.isBlank()) {
                String[] parts = name.split("\\s+");
                q.append(parts[0]);
                if (parts.length > 1) q.append(" ").append(parts[1]);
            }
            if (location != null && !location.isBlank()) q.append(" location:").append(location);

            String query = q.toString().trim();
            if (query.isBlank()) query = "type:user";

            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEARCH_URL + "?q=" + encoded + "&per_page=10"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("GitHub API error: status={}", response.statusCode());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) return results;

            for (JsonNode item : items) {
                RawSearchResult result = new RawSearchResult();
                result.setQuery(query);
                result.setSource(sourceName());
                result.setTitle(item.get("login").asText() + " - GitHub");
                result.setUrl(item.get("html_url").asText());
                result.setSnippet(item.has("type") ? "GitHub " + item.get("type").asText() : "GitHub user");
                result.setFetchedAt(Instant.now());
                enrichFromUserApi(result, item.get("url").asText());
                results.add(result);
            }

            log.info("GitHub search returned {} results for '{}'", results.size(), query);
        } catch (Exception e) {
            log.warn("GitHub search failed: {}", e.getMessage());
        }

        return results;
    }

    private void enrichFromUserApi(RawSearchResult result, String apiUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<String> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                JsonNode user = objectMapper.readTree(resp.body());
                String name = user.has("name") && !user.get("name").isNull()
                        ? user.get("name").asText() : null;
                String bio = user.has("bio") && !user.get("bio").isNull()
                        ? user.get("bio").asText() : null;
                String company = user.has("company") && !user.get("company").isNull()
                        ? user.get("company").asText() : null;
                String location = user.has("location") && !user.get("location").isNull()
                        ? user.get("location").asText() : null;
                String email = user.has("email") && !user.get("email").isNull()
                        ? user.get("email").asText() : null;

                if (name != null) result.setTitle(name + " - GitHub");
                StringBuilder snippet = new StringBuilder();
                if (bio != null) snippet.append(bio);
                if (company != null) snippet.append(" at ").append(company);
                if (location != null) snippet.append(" - ").append(location);
                if (email != null) snippet.append(" - ").append(email);
                if (!snippet.isEmpty()) result.setSnippet(snippet.toString());
            }
        } catch (Exception e) {
            log.debug("Failed to fetch GitHub user details: {}", e.getMessage());
        }
    }
}
