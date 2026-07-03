package com.example.SimleaBackendTest.repository;

import com.example.SimleaBackendTest.entity.SourceProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceProfileRepository extends JpaRepository<SourceProfile, UUID> {
    List<SourceProfile> findByFullNameContainingIgnoreCase(String name);
    Optional<SourceProfile> findBySourceAndProfileUrl(String source, String profileUrl);
}
