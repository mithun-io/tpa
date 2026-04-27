package com.tpa.repository;

import com.tpa.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {
    List<Claim> findByUserId(Long userId);
    List<Claim> findByCarrier_Id(Long carrierId);
    boolean existsByPolicyNumber(String policyNumber);

    @org.springframework.data.jpa.repository.Query("SELECT c.status, COUNT(c) FROM Claim c GROUP BY c.status")
    List<Object[]> countClaimsByStatus();

    @org.springframework.data.jpa.repository.Query("SELECT CAST(c.createdDate AS date), COUNT(c) FROM Claim c WHERE c.createdDate >= :startDate GROUP BY CAST(c.createdDate AS date) ORDER BY CAST(c.createdDate AS date)")
    List<Object[]> countClaimsPerDay(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(c.amount) FROM Claim c WHERE c.status = 'CARRIER_APPROVED'")
    Double sumApprovedClaimAmount();
}
