package com.example.SimleaBackendTest.service.extraction;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import com.example.SimleaBackendTest.entity.SourceProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityExtractor {

    private static final Logger log = LoggerFactory.getLogger(EntityExtractor.class);
    private static final Pattern NAME_PATTERN = Pattern.compile("^([A-Z][a-z]+)\\s+([A-Z][a-z]+)");
    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "(?:at|@|•|\\-)\\s*([A-Za-z0-9&.\\s]+?)(?:\\.|,|\\s-|\\s\\|)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINKEDIN_URL_PATTERN = Pattern.compile(
            "(https?://(?:www\\.)?linkedin\\.com/in/[^\\s/\\\"'>]+)");

    private final ObjectMapper objectMapper;

    public EntityExtractor() {
        this.objectMapper = new ObjectMapper();
    }

    public List<SourceProfile> extract(List<RawSearchResult> rawResults) {
        List<SourceProfile> profiles = new ArrayList<>();

        for (RawSearchResult raw : rawResults) {
            try {
                SourceProfile profile = extractSingle(raw);
                if (profile != null && profile.getFullName() != null) {
                    profiles.add(profile);
                }
            } catch (Exception e) {
                log.debug("Failed to extract from result '{}': {}", raw.getUrl(), e.getMessage());
            }
        }

        return profiles;
    }

    SourceProfile extractSingle(RawSearchResult raw) {
        SourceProfile profile = new SourceProfile();
        profile.setSource(raw.getSource());
        profile.setProfileUrl(raw.getUrl());
        profile.setFetchedAt(raw.getFetchedAt());

        String title = raw.getTitle();
        String snippet = raw.getSnippet();

        String extractedName = extractName(title, snippet);
        if (extractedName != null) {
            profile.setFullName(extractedName);
        }

        profile.setHeadline(extractHeadline(title, snippet));
        profile.setCompany(extractCompany(title, snippet));
        profile.setLocation(extractLocation(title, snippet));

        String linkedinUrl = extractLinkedInUrl(raw.getUrl());
        if (linkedinUrl != null) {
            profile.setProfileUrl(linkedinUrl);
        }

        try {
            profile.setRawJson(objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "snippet", snippet,
                    "originalUrl", raw.getUrl()
            )));
        } catch (JsonProcessingException e) {
            log.debug("Failed to serialize raw data", e);
        }

        log.debug("Extracted: name={}, company={}, headline={}, source={}",
                profile.getFullName(), profile.getCompany(), profile.getHeadline(), profile.getSource());

        return profile;
    }

    String extractName(String title, String snippet) {
        if (title != null) {
            String cleaned = title.replaceAll("\\s*\\|.*$", "").replaceAll("\\s*-\\s*LinkedIn$", "").trim();

            java.util.regex.Matcher m = NAME_PATTERN.matcher(cleaned);
            if (m.find()) {
                return cleaned.split("\\s*-\\s*|\\s*\\|\\s*|\\s*•\\s*")[0].trim();
            }
        }
        return null;
    }

    String extractHeadline(String title, String snippet) {
        if (title != null) {
            String[] separators = title.split("\\s*-\\s*|\\s*\\|\\s*|\\s*•\\s*");
            if (separators.length >= 2) {
                String candidate = separators[separators.length - 1].trim();
                if (candidate.length() < 100) {
                    return candidate;
                }
            }
        }
        if (snippet != null && !snippet.isBlank()) {
            int end = snippet.indexOf('.');
            return end > 0 ? snippet.substring(0, end).trim() : snippet.trim();
        }
        return null;
    }

    String extractCompany(String title, String snippet) {
        String text = (title != null ? title : "") + " " + (snippet != null ? snippet : "");
        java.util.regex.Matcher m = COMPANY_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    String extractLocation(String title, String snippet) {
        String text = (title != null ? title + " " : "") + (snippet != null ? snippet : "");
        Pattern locationPattern = Pattern.compile(
                "([A-Z][a-z]+(?:\\s[A-Z][a-z]+)?,\\s*[A-Z]{2})");
        java.util.regex.Matcher m = locationPattern.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    String extractLinkedInUrl(String url) {
        if (url == null) return null;
        java.util.regex.Matcher m = LINKEDIN_URL_PATTERN.matcher(url);
        if (m.find()) {
            String found = m.group(1);
            if (found.endsWith("/")) {
                found = found.substring(0, found.length() - 1);
            }
            return found;
        }
        return null;
    }
}
