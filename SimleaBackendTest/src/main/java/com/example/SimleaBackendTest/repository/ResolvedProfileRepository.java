package com.example.SimleaBackendTest.repository;

import com.example.SimleaBackendTest.entity.ResolvedProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResolvedProfileRepository extends JpaRepository<ResolvedProfile, UUID> {
    List<ResolvedProfile> findByFullNameContainingIgnoreCase(String name);
    List<ResolvedProfile> findByCompanyContainingIgnoreCase(String company);
}
