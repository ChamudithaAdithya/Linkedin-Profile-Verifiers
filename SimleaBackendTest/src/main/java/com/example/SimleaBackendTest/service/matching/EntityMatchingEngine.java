package com.example.SimleaBackendTest.service.matching;

import com.example.SimleaBackendTest.entity.ResolvedProfile;
import com.example.SimleaBackendTest.entity.SourceProfile;
import com.example.SimleaBackendTest.repository.SourceProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EntityMatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(EntityMatchingEngine.class);
    private static final int MERGE_THRESHOLD = 70;
    private static final int FLAG_THRESHOLD = 50;

    private final List<ScoringRule> rules;
    private final SourceProfileRepository sourceProfileRepository;
    private final ObjectMapper objectMapper;

    public EntityMatchingEngine(
            List<ScoringRule> rules,
            SourceProfileRepository sourceProfileRepository) {
        this.rules = rules;
        this.sourceProfileRepository = sourceProfileRepository;
        this.objectMapper = new ObjectMapper();
    }

    public List<ResolvedProfile> resolve(List<SourceProfile> extracted) {
        sourceProfileRepository.saveAll(extracted);

        List<ResolvedProfile> resolved = new ArrayList<>();
        Map<String, List<SourceProfile>> groups = groupByNameBlock(extracted);

        for (var entry : groups.entrySet()) {
            List<SourceProfile> group = entry.getValue();
            if (group.size() == 1) {
                resolved.add(buildResolved(group, 100.0));
            } else {
                var merged = mergeGroup(group);
                if (merged != null) {
                    resolved.add(merged);
                }
            }
        }

        log.info("Resolved {} profiles from {} source profiles across {} name blocks",
                resolved.size(), extracted.size(), groups.size());
        return resolved;
    }

    public MergeDecision evaluate(SourceProfile a, SourceProfile b) {
        var pair = new ProfilePair(a, b);
        int score = rules.stream().mapToInt(r -> r.score(pair)).sum();
        if (score >= MERGE_THRESHOLD) return MergeDecision.MERGE;
        if (score >= FLAG_THRESHOLD) return MergeDecision.FLAG;
        return MergeDecision.SKIP;
    }

    private Map<String, List<SourceProfile>> groupByNameBlock(List<SourceProfile> profiles) {
        return profiles.stream()
                .filter(p -> p.getFullName() != null)
                .collect(Collectors.groupingBy(
                    p -> p.getFullName().toLowerCase().replaceAll("[^a-z]", "")
                ));
    }

    private ResolvedProfile mergeGroup(List<SourceProfile> group) {
        if (group.isEmpty()) return null;

        List<SourceProfile> merged = new ArrayList<>();
        merged.add(group.getFirst());

        for (int i = 1; i < group.size(); i++) {
            SourceProfile candidate = group.get(i);
            boolean matched = false;

            for (int j = 0; j < merged.size(); j++) {
                var decision = evaluate(merged.get(j), candidate);
                if (decision == MergeDecision.MERGE) {
                    merged.set(j, mergePair(merged.get(j), candidate));
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                merged.add(candidate);
            }
        }

        return merged.stream()
                .map(m -> buildResolved(List.of(m), computeConfidence(m)))
                .findFirst()
                .orElse(null);
    }

    ResolvedProfile buildResolved(List<SourceProfile> sources, double confidence) {
        ResolvedProfile profile = new ResolvedProfile();
        if (sources.isEmpty()) return profile;

        SourceProfile primary = sources.getFirst();
        profile.setFullName(primary.getFullName());
        profile.setHeadline(primary.getHeadline());
        profile.setCompany(primary.getCompany());
        profile.setLocation(primary.getLocation());
        profile.setPhotoUrl(primary.getPhotoUrl());

        for (SourceProfile s : sources) {
            if (s.getProfileUrl() != null && s.getProfileUrl().contains("linkedin.com")) {
                profile.setLinkedinUrl(s.getProfileUrl());
            }
            if (s.getProfileUrl() != null && s.getProfileUrl().contains("github.com")) {
                profile.setGithubUrl(s.getProfileUrl());
            }
            if (s.getProfileUrl() != null && !s.getProfileUrl().contains("linkedin.com")
                    && !s.getProfileUrl().contains("github.com")) {
                profile.setWebsiteUrl(s.getProfileUrl());
            }
            if (profile.getPhotoUrl() == null && s.getPhotoUrl() != null) {
                profile.setPhotoUrl(s.getPhotoUrl());
            }
        }

        profile.setConfidence(confidence);

        List<String> ids = sources.stream()
                .map(s -> s.getId().toString())
                .toList();
        profile.setSourceIds(String.join(",", ids));

        try {
            profile.setRawData(objectMapper.writeValueAsString(
                    sources.stream().map(s -> Map.of(
                            "id", s.getId().toString(),
                            "source", s.getSource(),
                            "url", s.getProfileUrl()
                    )).toList()));
        } catch (JsonProcessingException e) {
            log.debug("Failed to serialize rawData", e);
        }

        profile.getUpdatedAt();
        return profile;
    }

    private SourceProfile mergePair(SourceProfile a, SourceProfile b) {
        if (b.getFullName() != null) a.setFullName(b.getFullName());
        if (b.getHeadline() != null) a.setHeadline(b.getHeadline());
        if (b.getCompany() != null) a.setCompany(b.getCompany());
        if (b.getLocation() != null) a.setLocation(b.getLocation());
        if (b.getProfileUrl() != null) a.setProfileUrl(b.getProfileUrl());
        if (b.getPhotoUrl() != null) a.setPhotoUrl(b.getPhotoUrl());
        return a;
    }

    private double computeConfidence(SourceProfile profile) {
        double score = 0.0;
        int checks = 0;

        if (profile.getFullName() != null && profile.getFullName().split("\\s+").length >= 2) {
            score += 30;
        }
        checks += 30;

        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            score += 20;
        }
        checks += 20;

        if (profile.getCompany() != null && !profile.getCompany().isBlank()) {
            score += 20;
        }
        checks += 20;

        if (profile.getLocation() != null && !profile.getLocation().isBlank()) {
            score += 10;
        }
        checks += 10;

        if (profile.getPhotoUrl() != null) {
            score += 10;
        }
        checks += 10;

        if (profile.getProfileUrl() != null && profile.getProfileUrl().contains("linkedin.com")) {
            score += 10;
        }
        checks += 10;

        return checks > 0 ? score / checks * 100.0 : 0.0;
    }
}
