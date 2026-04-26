package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.mapper.ClaimMapper;
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
                .status(ClaimStatus.PENDING)
                .amount(request.getClaimedAmount())
                .carrierName(request.getCarrierName())
                .carrier(carrier)
                .build();
        
        claim = claimRepository.save(claim);
        log.info("Claim {} created with status PENDING. Upload both documents to trigger processing.", claim.getId());
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
    @Transactional
    @CacheEvict(value = "claims", key = "#claimId")
    public void processClaimDecision(Long claimId, ClaimDecisionResponse decision) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        ClaimStatus previousStatus = claim.getStatus();
        log.info("Processing claim {} — current status: {}, decision: {}", claimId, previousStatus, decision.getStatus());

        // Skip if already in a terminal or processed state
        if (previousStatus == ClaimStatus.APPROVED || previousStatus == ClaimStatus.REJECTED
                || previousStatus == ClaimStatus.REVIEW || previousStatus == ClaimStatus.PROCESSING) {
            log.warn("Claim {} is already in {} state. Skipping duplicate processing.", claimId, previousStatus);
            return;
        }

        // Validate PENDING → PROCESSING transition
        claimStateMachine.validateTransition(previousStatus, ClaimStatus.PROCESSING);

        // Immediately persist as PROCESSING
        claim.setStatus(ClaimStatus.PROCESSING);
        claimRepository.save(claim);
        auditLogService.logAction(claimId, "STATUS_PROCESSING", previousStatus, ClaimStatus.PROCESSING);
        log.info("Claim {} moved to PROCESSING", claimId);

        // Validate PROCESSING → final decision status
        claimStateMachine.validateTransition(ClaimStatus.PROCESSING, decision.getStatus());

        claim.setStatus(decision.getStatus());
        claim.setProcessedDate(LocalDateTime.now());

        if (decision.getReasons() != null && !decision.getReasons().isEmpty()) {
            claim.setRejectionReason(String.join(", ", decision.getReasons()));
        }

        claimRepository.save(claim);
        auditLogService.logAction(claimId, "RULE_ENGINE_DECISION", ClaimStatus.PROCESSING, claim.getStatus());
        log.info("Claim {} final status: {}", claimId, claim.getStatus());
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
        return ((com.tpa.repository.ClaimAuditRepository) org.springframework.web.context.support.WebApplicationContextUtils
            .getWebApplicationContext(((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getServletContext())
            .getBean(com.tpa.repository.ClaimAuditRepository.class)).findByClaimIdOrderByChangedAtDesc(claimId);
    }
}
