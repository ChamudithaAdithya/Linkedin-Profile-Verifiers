package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.config.SearchProperties;
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
public class HunterEnrichmentConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(HunterEnrichmentConnector.class);
    private static final String EMAIL_FINDER_URL = "https://api.hunter.io/v2/email-finder";

    private final SearchProperties searchProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HunterEnrichmentConnector(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "hunter";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        return List.of();
    }

    @Override
    public List<RawSearchResult> searchStructured(String name, String company, String location) {
        String apiKey = searchProperties.getHunter().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        if (company == null || company.isBlank() || name == null || name.isBlank()) {
            return List.of();
        }

        List<RawSearchResult> results = new ArrayList<>();

        try {
            String[] nameParts = name.split("\\s+");
            if (nameParts.length < 2) return results;

            String firstName = URLEncoder.encode(nameParts[0], StandardCharsets.UTF_8);
            String lastName = URLEncoder.encode(nameParts[nameParts.length - 1], StandardCharsets.UTF_8);
            String domain = URLEncoder.encode(company.toLowerCase().replaceAll("[^a-z0-9.]", "") + ".com",
                    StandardCharsets.UTF_8);

            String url = EMAIL_FINDER_URL + "?domain=" + domain
                    + "&first_name=" + firstName
                    + "&last_name=" + lastName
                    + "&api_key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Hunter API error: status={}", response.statusCode());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null) return results;

            String email = data.has("email") && !data.get("email").isNull()
                    ? data.get("email").asText() : null;
            int score = data.has("score") ? data.get("score").asInt() : 0;
            String source = data.has("sources") && data.get("sources").isArray()
                    ? data.get("sources").get(0).get("domain").asText() : null;

            if (email != null) {
                RawSearchResult result = new RawSearchResult();
                result.setQuery(name + " " + company);
                result.setSource(sourceName());
                result.setTitle("Email: " + email);
                result.setSnippet(String.format("Confidence: %d%% | Source: %s", score, source != null ? source : "unknown"));
                result.setUrl("https://hunter.io/verify/" + email);
                result.setFetchedAt(Instant.now());
                results.add(result);
            }

            log.info("Hunter lookup for {} at {}: email={}, score={}", name, company, email, score);
        } catch (Exception e) {
            log.warn("Hunter enrichment failed: {}", e.getMessage());
        }

        return results;
    }
}
