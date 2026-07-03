package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(MockSourceConnector.class);

    @Override
    public String sourceName() {
        return "mock";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        log.info("MockSourceConnector returning sample data for query: '{}'", query);
        return List.of(
            buildResult(query, "LinkedIn",
                "Chamuditha Adithya - Software Engineer - Google | LinkedIn",
                "Experienced software engineer specializing in full-stack development and distributed systems at Google.",
                "https://linkedin.com/in/chamuditha-adithya"),
            buildResult(query, "LinkedIn",
                "Chamuditha Adithya - Senior Engineer - Microsoft | LinkedIn",
                "Senior software engineer with expertise in cloud infrastructure, Java, and Angular.",
                "https://linkedin.com/in/chamuditha-adithya-2"),
            buildResult(query, "GitHub",
                "Chamuditha Adithya - Overview",
                "Full-stack developer. Contributes to open source projects. Tech lead at Google.",
                "https://github.com/chamudithaadithya"),
            buildResult(query, "Google",
                "Chamuditha Adithya Google Profile",
                "Software Engineer at Google. Java, Spring Boot, Angular, distributed systems.",
                "https://example.com/profile/chamuditha-adithya"),
            buildResult(query, "StackOverflow",
                "Chamuditha Adithya - Stack Overflow",
                "Top contributor in Java and Spring Boot tags.",
                "https://stackoverflow.com/users/chamuditha-adithya")
        );
    }

    private RawSearchResult buildResult(String query, String source, String title, String snippet, String url) {
        RawSearchResult result = new RawSearchResult();
        result.setQuery(query);
        result.setSource(source);
        result.setTitle(title);
        result.setSnippet(snippet);
        result.setUrl(url);
        result.setFetchedAt(Instant.now());
        return result;
    }
}
