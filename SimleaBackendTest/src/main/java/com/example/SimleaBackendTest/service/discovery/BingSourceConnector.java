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
public class BingSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(BingSourceConnector.class);
    private static final String API_URL = "https://api.bing.microsoft.com/v7.0/search";

    private final SearchProperties searchProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BingSourceConnector(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "bing";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        List<RawSearchResult> results = new ArrayList<>();
        String apiKey = searchProperties.getBing().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Bing API key not configured");
            return results;
        }

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_URL + "?q=" + encoded + "&count=10&mkt=en-US";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Bing API error: status={}, body={}", response.statusCode(), response.body());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode webPages = root.get("webPages");
            if (webPages == null) return results;

            JsonNode items = webPages.get("value");
            if (items == null || !items.isArray()) return results;

            for (JsonNode item : items) {
                RawSearchResult result = new RawSearchResult();
                result.setQuery(query);
                result.setSource(sourceName());
                result.setTitle(optText(item, "name"));
                result.setSnippet(optText(item, "snippet"));
                result.setUrl(optText(item, "url"));
                result.setFetchedAt(Instant.now());
                results.add(result);
            }

            log.info("Bing search for '{}' returned {} results", query, results.size());
        } catch (Exception e) {
            log.error("Bing search failed for query '{}'", query, e);
        }

        return results;
    }

    private String optText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asText(null) : null;
    }
}
