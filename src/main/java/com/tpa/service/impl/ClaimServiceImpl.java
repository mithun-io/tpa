package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.mapper.ClaimMapper;
import com.tpa.kafka.producer.ProducerService;
import com.tpa.kafka.event.ClaimNotificationEvent;
import com.tpa.repository.ClaimRepository;
import com.tpa.repository.ClaimSpecification;
import com.tpa.repository.UserRepository;
import com.tpa.repository.CarrierRepository;
import com.tpa.entity.Carrier;
import com.tpa.service.AuditLogService;
import com.tpa.service.ClaimService;
import com.tpa.service.ClaimStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ClaimMapper claimMapper;
    private final ClaimEventProducer claimEventProducer;
    private final AuditLogService auditLogService;
    private final ClaimStateMachine claimStateMachine;
    private final CarrierRepository carrierRepository;
    private final ProducerService producerService;
    private final com.tpa.repository.ClaimAuditRepository claimAuditRepository;

    @Override
    public ClaimResponse createClaim(ClaimDataRequest request, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Carrier carrier = null;
        if (request.getCarrierName() != null && !request.getCarrierName().isEmpty()) {
            carrier = carrierRepository.findByCompanyNameIgnoreCase(request.getCarrierName()).orElse(null);
        }

        Claim claim = Claim.builder()
                .policyNumber(request.getPolicyNumber() != null ? request.getPolicyNumber() : "TEMP-" + System.currentTimeMillis())
                .user(user)
                .status(ClaimStatus.SUBMITTED)
                .amount(request.getClaimedAmount())
                .carrierName(request.getCarrierName())
                .carrier(carrier)
                .patientName(request.getClaimFormPatientName())
                .hospitalName(request.getClaimFormHospitalName())
                .admissionDate(request.getClaimFormAdmissionDate())
                .dischargeDate(request.getClaimFormDischargeDate())
                .totalBillAmount(request.getTotalBillAmount())
                .policyId(request.getPolicyId())
                .policyName(request.getPolicyName())
                .claimType(request.getClaimType())
                .diagnosis(request.getDiagnosis())
                .billNumber(request.getBillNumber())
                .billDate(request.getBillDate())
                .build();
        
        claim = claimRepository.save(claim);
        log.info("Claim {} created with status SUBMITTED. Upload both documents to trigger processing.", claim.getId());
        auditLogService.logAction(claim.getId(), "CLAIM_CREATED", null, claim.getStatus());

        return claimMapper.toDto(claim);
    }

    @Override
    public ClaimResponse getClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        return claimMapper.toDto(claim);
    }

    @Override
    public List<ClaimResponse> getAllClaims() {
        return claimMapper.toDtoList(claimRepository.findAll());
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @CacheEvict(value = "claims", key = "#claimId")
    public void processClaimDecision(Long claimId, ClaimDecisionResponse decision) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        ClaimStatus previousStatus = claim.getStatus();
        log.info("[CLAIM-SYNC] Processing decision for claim {}. Current status: {}, New status: {}", 
                claimId, previousStatus, decision.getStatus());

        try {
            // Skip if already in a terminal or processed state
            if (previousStatus == ClaimStatus.CARRIER_APPROVED || previousStatus == ClaimStatus.REJECTED
                    || previousStatus == ClaimStatus.SETTLED) {
                log.warn("[CLAIM-SYNC] Claim {} is in terminal state {}. Skipping decision.", claimId, previousStatus);
                return;
            }

            // Validate current -> AI_VALIDATED transition
            claimStateMachine.validateTransition(previousStatus, ClaimStatus.AI_VALIDATED);
            
            // Mark as AI_VALIDATED internally before applying final decision
            claim.setStatus(ClaimStatus.AI_VALIDATED);
            claimRepository.save(claim);
            auditLogService.logAction(claimId, "AI_VALIDATION_PASSED", previousStatus, ClaimStatus.AI_VALIDATED);

            // Validate AI_VALIDATED -> final decision status
            claimStateMachine.validateTransition(ClaimStatus.AI_VALIDATED, decision.getStatus());

            claim.setStatus(decision.getStatus());
            claim.setProcessedDate(LocalDateTime.now());

            if (decision.getReasons() != null && !decision.getReasons().isEmpty()) {
                claim.setRejectionReason(String.join(", ", decision.getReasons()));
            }

            claimRepository.save(claim);
            auditLogService.logAction(claimId, "RULE_ENGINE_DECISION", ClaimStatus.AI_VALIDATED, claim.getStatus());
            log.info("[CLAIM-SYNC] Claim {} successfully moved to {}", claimId, claim.getStatus());
            
            // Send notification (async)
            ClaimNotificationEvent notificationEvent = ClaimNotificationEvent.builder()
                    .claimId(claim.getId())
                    .policyNumber(claim.getPolicyNumber())
                    .customerEmail(claim.getUser().getEmail())
                    .status(claim.getStatus())
                    .message("Status: " + claim.getStatus() + ". Notes: " + (claim.getRejectionReason() != null ? claim.getRejectionReason() : "N/A"))
                    .build();
            producerService.sendClaimNotificationEvent(notificationEvent);

        } catch (Exception e) {
            log.error("[CLAIM-SYNC] CRITICAL: Status transition failed for claim {}: {}", claimId, e.getMessage());
            // We throw to ensure the REQUIRES_NEW transaction rolls back, 
            // but the caller (FileUploadService) will catch it.
            throw e; 
        }
    }

    @Override
    public Page<ClaimResponse> searchClaims(ClaimStatus status, LocalDateTime from, LocalDateTime to, Double minAmount, Double maxAmount, String username, Pageable pageable) {
        Specification<Claim> spec = Specification.where(ClaimSpecification.hasStatus(status))
                .and(ClaimSpecification.createdBetween(from, to))
                .and(ClaimSpecification.amountBetween(minAmount, maxAmount))
                .and(ClaimSpecification.hasUser(username));

        return claimRepository.findAll(spec, pageable).map(claimMapper::toDto);
    }

    @Override
    public List<com.tpa.entity.ClaimAudit> getClaimAudits(Long claimId) {
        return claimAuditRepository.findByClaimIdOrderByChangedAtDesc(claimId);
    }
}
