package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import com.example.SimleaBackendTest.repository.RawSearchResultRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final List<SourceConnector> connectors;
    private final RawSearchResultRepository rawRepository;

    public DiscoveryService(
            List<SourceConnector> connectors,
            RawSearchResultRepository rawRepository) {
        this.connectors = connectors;
        this.rawRepository = rawRepository;
        log.info("DiscoveryService initialized with {} connector(s)", connectors.size());
    }

    public List<RawSearchResult> discover(String query) {
        List<String> queries = buildQueryVariants(query);
        List<RawSearchResult> allResults = Collections.synchronizedList(new ArrayList<>());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();

            for (SourceConnector connector : connectors) {
                for (String q : queries) {
                    futures.add(executor.submit(() -> {
                        try {
                            List<RawSearchResult> results = connector.search(q);
                            allResults.addAll(results);
                        } catch (Exception e) {
                            log.warn("Connector '{}' failed for query '{}': {}",
                                    connector.sourceName(), q, e.getMessage());
                        }
                    }));
                }
            }

            for (var future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.warn("Async search task failed: {}", e.getMessage());
                }
            }
        }

        log.info("Discovery for '{}' returned {} raw results across {} query variants",
                query, allResults.size(), queries.size());

        return rawRepository.saveAll(allResults);
    }

    public List<RawSearchResult> discoverStructured(String name, String company, String location) {
        List<RawSearchResult> allResults = Collections.synchronizedList(new ArrayList<>());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();

            for (SourceConnector connector : connectors) {
                futures.add(executor.submit(() -> {
                    try {
                        List<RawSearchResult> results = connector.searchStructured(name, company, location);
                        allResults.addAll(results);
                    } catch (Exception e) {
                        log.warn("Connector '{}' failed for structured search: {}",
                                connector.sourceName(), e.getMessage());
                    }
                }));
            }

            for (var future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.warn("Async search task failed: {}", e.getMessage());
                }
            }
        }

        log.info("Structured discovery for name='{}', company='{}', location='{}' returned {} results",
                name, company, location, allResults.size());

        return rawRepository.saveAll(allResults);
    }

    private List<String> buildQueryVariants(String userQuery) {
        String trimmed = userQuery.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }

        List<String> variants = new ArrayList<>();
        variants.add(trimmed);

        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 2) {
            String name = parts[0];
            String company = parts[1];

            variants.add("\"" + trimmed + "\"" + " LinkedIn");
            variants.add(name + " " + company + " profile");
        }

        String nameOnly = parts[0];
        if (!nameOnly.equals(trimmed)) {
            variants.add("\"" + nameOnly + "\"" + " " + parts[1]);
        }

        return variants;
    }
}
