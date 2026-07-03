package com.example.SimleaBackendTest.service.matching;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailMatchRule implements ScoringRule {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    @Override
    public int score(ProfilePair pair) {
        String emailA = extractEmail(pair.a().getRawJson());
        String emailB = extractEmail(pair.b().getRawJson());
        if (emailA == null || emailB == null) return 0;
        return emailA.equalsIgnoreCase(emailB) ? 30 : 0;
    }

    private String extractEmail(String rawJson) {
        if (rawJson == null) return null;
        var m = EMAIL_PATTERN.matcher(rawJson);
        return m.find() ? m.group() : null;
    }

    @Override
    public String name() {
        return "email-match";
    }
}
