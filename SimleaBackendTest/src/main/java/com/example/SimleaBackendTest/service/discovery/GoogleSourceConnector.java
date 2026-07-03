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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("search.google.api-key")
public class GoogleSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(GoogleSourceConnector.class);
    private static final String API_URL = "https://www.googleapis.com/customsearch/v1";

    private final SearchProperties searchProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleSourceConnector(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "google";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        List<RawSearchResult> results = new ArrayList<>();

        String apiKey = searchProperties.getGoogle().getApiKey();
        String cx = searchProperties.getGoogle().getCx();

        if (apiKey == null || apiKey.isBlank() || cx == null || cx.isBlank()) {
            log.warn("Google Custom Search API key or CX not configured");
            return results;
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_URL + "?key=" + apiKey + "&cx=" + cx + "&q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Google Search API error: status={}, body={}",
                        response.statusCode(), response.body());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) {
                return results;
            }

            for (JsonNode item : items) {
                RawSearchResult result = new RawSearchResult();
                result.setQuery(query);
                result.setSource(sourceName());
                result.setTitle(optText(item, "title"));
                result.setSnippet(optText(item, "snippet"));
                result.setUrl(optText(item, "link"));
                result.setFetchedAt(Instant.now());
                results.add(result);
            }

            log.info("Google search for '{}' returned {} results", query, results.size());
        } catch (Exception e) {
            log.error("Google search failed for query '{}'", query, e);
        }

        return results;
    }

    private String optText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asText(null) : null;
    }
}
