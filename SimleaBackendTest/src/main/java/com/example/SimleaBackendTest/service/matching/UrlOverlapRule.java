package com.example.SimleaBackendTest.service.matching;

import org.springframework.stereotype.Component;

@Component
public class UrlOverlapRule implements ScoringRule {

    @Override
    public int score(ProfilePair pair) {
        String ua = pair.urlA();
        String ub = pair.urlB();
        if (ua == null || ub == null) return 0;

        String normalizedA = normalize(ua);
        String normalizedB = normalize(ub);

        if (normalizedA.equals(normalizedB)) return 10;
        return 0;
    }

    private String normalize(String url) {
        return url.toLowerCase()
                .replaceAll("https?://(www\\.)?", "")
                .replaceAll("/$", "")
                .trim();
    }

    @Override
    public String name() {
        return "url-overlap";
    }
}
