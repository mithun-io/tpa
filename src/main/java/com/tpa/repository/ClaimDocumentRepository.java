package com.tpa.repository;

import com.tpa.entity.Claim;
import com.tpa.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    List<ClaimDocument> findByClaimId(Long claimId);
    List<ClaimDocument> findByClaim(Claim claim);
}
