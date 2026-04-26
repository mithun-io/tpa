package com.tpa.repository;

import com.tpa.entity.ClaimAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimAuditRepository extends JpaRepository<ClaimAudit, Long> {
    List<ClaimAudit> findByClaimIdOrderByChangedAtDesc(Long claimId);
}
