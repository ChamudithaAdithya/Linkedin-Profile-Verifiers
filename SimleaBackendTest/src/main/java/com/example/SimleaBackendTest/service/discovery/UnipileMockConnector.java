package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UnipileMockConnector implements AccountConnector {

    private static final Logger log = LoggerFactory.getLogger(UnipileMockConnector.class);

    @Override
    public String sourceName() {
        return "unipile-mock";
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String accountLabel() {
        return "LinkedIn Account (Mock)";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        log.info("UnipileMock search for: '{}'", query);
        return searchProfiles(query, null, null);
    }

    @Override
    public List<RawSearchResult> searchStructured(String name, String company, String location) {
        log.info("UnipileMock structured search: name='{}', company='{}', location='{}'",
                name, company, location);
        return searchProfiles(name, company, location);
    }

    @Override
    public List<RawSearchResult> searchProfiles(String name, String company, String location) {
        List<RawSearchResult> results = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            results.add(buildResult(name,
                    name + " - Software Engineer - " + (company != null ? company : "Google") + " | LinkedIn",
                    "Experienced software engineer. Full-stack development, distributed systems, cloud infrastructure.",
                    "https://linkedin.com/in/" + name.toLowerCase().replace(' ', '-')));

            results.add(buildResult(name,
                    name + " - Senior Engineer - " + (company != null ? company : "Microsoft") + " | LinkedIn",
                    "Senior software engineer. Java, Spring Boot, Angular, cloud architecture.",
                    "https://linkedin.com/in/" + name.toLowerCase().replace(' ', '-') + "-2"));
        }

        if (company != null && !company.isBlank()) {
            String searchName = name != null ? name : "Professional";
            results.add(buildResult(searchName + " " + company,
                    searchName + " - " + company + " Company Profile",
                    searchName + " works at " + company + ". Role: Software Engineer. Location: "
                            + (location != null ? location : "Colombo, Sri Lanka"),
                    "https://" + company.toLowerCase() + ".com/team/" + searchName.toLowerCase().replace(' ', '-')));
        }

        if (location != null && !location.isBlank()) {
            String searchName = name != null ? name : "Professional";
            results.add(buildResult(searchName + " " + location,
                    searchName + " - " + (company != null ? company : "Tech Company") + " | " + location,
                    "Based in " + location + ". Full-stack developer with 5+ years experience.",
                    "https://linkedin.com/in/" + searchName.toLowerCase().replace(' ', '-') + "-" + location.toLowerCase().replace(' ', '-')));
        }

        if (name != null) {
            results.add(buildResult(name,
                    name + " - GitHub",
                    "Full-stack developer. Open source contributions. Tech lead.",
                    "https://github.com/" + name.toLowerCase().replace(" ", "") + "/" + name.toLowerCase().replace(' ', '-')));

            results.add(buildResult(name,
                    name + " - Stack Overflow",
                    "Top contributor. Java, Spring Boot, Angular.",
                    "https://stackoverflow.com/users/" + name.toLowerCase().replace(' ', '-')));
        }

        log.info("UnipileMock returning {} results for name='{}', company='{}', location='{}'",
                results.size(), name, company, location);
        return results;
    }

    private RawSearchResult buildResult(String query, String title, String snippet, String url) {
        RawSearchResult result = new RawSearchResult();
        result.setQuery(query);
        result.setSource(sourceName());
        result.setTitle(title);
        result.setSnippet(snippet);
        result.setUrl(url);
        result.setFetchedAt(Instant.now());
        return result;
    }
}
