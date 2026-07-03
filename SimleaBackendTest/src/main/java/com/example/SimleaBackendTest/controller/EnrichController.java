package com.example.SimleaBackendTest.controller;

import com.example.SimleaBackendTest.entity.ResolvedProfile;
import com.example.SimleaBackendTest.service.enrichment.EnrichmentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrich")
public class EnrichController {

    private static final Logger log = LoggerFactory.getLogger(EnrichController.class);

    private final EnrichmentService enrichmentService;

    public EnrichController(EnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location) {

        try {
            List<ResolvedProfile> results;

            boolean hasStructured = (name != null && !name.isBlank())
                    || (company != null && !company.isBlank())
                    || (location != null && !location.isBlank());

            if (hasStructured) {
                results = enrichmentService.searchAndEnrichStructured(name, company, location);
            } else if (q != null && !q.isBlank()) {
                results = enrichmentService.searchAndEnrich(q);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Provide a text query 'q', or structured params 'name', 'company', 'location'"));
            }

            return ResponseEntity.ok(Map.of(
                    "results", results,
                    "total", results.size()
            ));
        } catch (Exception e) {
            log.error("Enrichment search failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable UUID id) {
        return enrichmentService.getProfileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refresh/{id}")
    public ResponseEntity<?> refreshProfile(@PathVariable UUID id) {
        return enrichmentService.refreshProfile(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
