package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebsiteConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(WebsiteConnector.class);

    @Override
    public String sourceName() {
        return "website";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        return List.of();
    }

    public RawSearchResult fetchPage(String url, String sourceQuery) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; SimleaBot/1.0)")
                    .timeout(8000)
                    .get();

            RawSearchResult result = new RawSearchResult();
            result.setQuery(sourceQuery);
            result.setSource(sourceName());
            result.setTitle(doc.title());
            result.setSnippet(doc.select("meta[name=description]").attr("content"));
            result.setUrl(url);
            result.setFetchedAt(Instant.now());
            return result;
        } catch (Exception e) {
            log.debug("Failed to fetch page {}: {}", url, e.getMessage());
            return null;
        }
    }

    public List<RawSearchResult> enrichWithPageMetadata(List<RawSearchResult> rawResults, String query) {
        List<RawSearchResult> enriched = new ArrayList<>();
        for (RawSearchResult result : rawResults) {
            if (result.getUrl() != null && shouldFetch(result.getUrl())) {
                RawSearchResult pageData = fetchPage(result.getUrl(), query);
                if (pageData != null) {
                    enriched.add(pageData);
                }
            }
        }
        return enriched;
    }

    private boolean shouldFetch(String url) {
        return url.startsWith("https://")
                && !url.contains("linkedin.com")     // skip LinkedIn directly
                && !url.contains("facebook.com")
                && !url.contains("twitter.com");
    }
}
