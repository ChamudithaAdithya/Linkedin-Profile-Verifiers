package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.util.List;

public interface SourceConnector {
    String sourceName();
    List<RawSearchResult> search(String query);

    default List<RawSearchResult> searchStructured(String name, String company, String location) {
        StringBuilder q = new StringBuilder();
        if (name != null && !name.isBlank()) q.append(name);
        if (company != null && !company.isBlank()) q.append(" ").append(company);
        if (location != null && !location.isBlank()) q.append(" ").append(location);
        return search(q.toString().trim());
    }
}
