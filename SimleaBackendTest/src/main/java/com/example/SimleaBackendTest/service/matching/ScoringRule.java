package com.example.SimleaBackendTest.service.matching;

public interface ScoringRule {
    int score(ProfilePair pair);
    String name();
}
