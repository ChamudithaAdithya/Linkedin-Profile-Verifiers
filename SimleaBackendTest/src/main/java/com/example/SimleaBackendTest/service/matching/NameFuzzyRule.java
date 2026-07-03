package com.example.SimleaBackendTest.service.matching;

import org.springframework.stereotype.Component;

@Component
public class NameFuzzyRule implements ScoringRule {

    @Override
    public int score(ProfilePair pair) {
        String nameA = pair.nameA();
        String nameB = pair.nameB();
        if (nameA == null || nameB == null) return 0;

        String a = nameA.toLowerCase().trim();
        String b = nameB.toLowerCase().trim();

        int distance = levenshtein(a, b);
        if (distance == 0) return 0;
        if (distance <= 2) return 25;
        if (distance <= 3 && a.split("\\s+").length >= 2 && b.split("\\s+").length >= 2) return 10;
        return 0;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    @Override
    public String name() {
        return "name-fuzzy";
    }
}
