package com.example.SimleaBackendTest.repository;

import com.example.SimleaBackendTest.entity.LinkedinAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkedinAccountRepository extends JpaRepository<LinkedinAccount, Long> {
    Optional<LinkedinAccount> findTopByValidTrueOrderByLastValidatedAtDesc();
}
