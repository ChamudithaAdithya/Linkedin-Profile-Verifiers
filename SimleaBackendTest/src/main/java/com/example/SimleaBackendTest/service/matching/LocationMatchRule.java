package com.example.SimleaBackendTest.service.matching;

import org.springframework.stereotype.Component;

@Component
public class LocationMatchRule implements ScoringRule {

    @Override
    public int score(ProfilePair pair) {
        String la = normalize(pair.locationA());
        String lb = normalize(pair.locationB());
        if (la == null || lb == null) return 0;

        if (la.equals(lb)) return 10;
        String cityA = la.split(",")[0].trim();
        String cityB = lb.split(",")[0].trim();
        if (cityA.equals(cityB) && !cityA.isBlank()) return 5;

        return 0;
    }

    private String normalize(String s) {
        if (s == null) return null;
        return s.toLowerCase().trim();
    }

    @Override
    public String name() {
        return "location-match";
    }
}
