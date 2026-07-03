package com.example.SimleaBackendTest.service.matching;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TitleOverlapRule implements ScoringRule {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "at", "in", "of", "for", "and", "or", "to", "with", "&");

    @Override
    public int score(ProfilePair pair) {
        Set<String> tokensA = tokenize(pair.headlineA());
        Set<String> tokensB = tokenize(pair.headlineB());

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;

        long intersection = tokensA.stream().filter(tokensB::contains).count();
        if (intersection >= 2) return 15;
        if (intersection == 1) return 5;
        return 0;
    }

    private Set<String> tokenize(String headline) {
        if (headline == null) return Set.of();
        return new HashSet<>(Arrays.asList(
                headline.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+")));
    }

    @Override
    public String name() {
        return "title-overlap";
    }
}
