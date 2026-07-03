package com.example.SimleaBackendTest.service.matching;

import org.springframework.stereotype.Component;

@Component
public class CompanyMatchRule implements ScoringRule {

    @Override
    public int score(ProfilePair pair) {
        String ca = normalize(pair.companyA());
        String cb = normalize(pair.companyB());
        if (ca == null || cb == null) return 0;
        return ca.equals(cb) || ca.contains(cb) || cb.contains(ca) ? 20 : 0;
    }

    private String normalize(String s) {
        if (s == null) return null;
        return s.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replace("inc", "")
                .replace("llc", "")
                .replace("ltd", "")
                .trim();
    }

    @Override
    public String name() {
        return "company-match";
    }
}
