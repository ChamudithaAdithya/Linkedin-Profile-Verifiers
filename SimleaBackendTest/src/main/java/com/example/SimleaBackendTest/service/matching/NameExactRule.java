package com.example.SimleaBackendTest.service.matching;

import org.springframework.stereotype.Component;

@Component
public class NameExactRule implements ScoringRule {

    @Override
    public int score(ProfilePair pair) {
        String nameA = pair.nameA();
        String nameB = pair.nameB();
        if (nameA == null || nameB == null) return 0;
        return nameA.equalsIgnoreCase(nameB) ? 40 : 0;
    }

    @Override
    public String name() {
        return "name-exact";
    }
}
