package com.tpa.service;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ClaimService {
    ClaimResponse createClaim(ClaimDataRequest request, String username);
    ClaimResponse getClaim(Long claimId);
    List<ClaimResponse> getAllClaims();
    void processClaimDecision(Long claimId, ClaimDecisionResponse decision);
    
    Page<ClaimResponse> searchClaims(ClaimStatus status, LocalDateTime from, LocalDateTime to, Double minAmount, Double maxAmount, String username, Pageable pageable);
    
    List<com.tpa.entity.ClaimAudit> getClaimAudits(Long claimId);
}
