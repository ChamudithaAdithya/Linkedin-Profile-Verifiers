package com.example.SimleaBackendTest.repository;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawSearchResultRepository extends JpaRepository<RawSearchResult, UUID> {
    List<RawSearchResult> findByQuery(String query);
    List<RawSearchResult> findByQueryAndSource(String query, String source);
    void deleteByQuery(String query);
}
