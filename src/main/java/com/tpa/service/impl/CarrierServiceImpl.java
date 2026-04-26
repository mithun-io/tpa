package com.tpa.service.impl;

import com.tpa.dto.response.ClaimResponse;
import com.tpa.dto.response.PolicyStatusResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.BadRequestException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.mapper.ClaimMapper;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.service.CarrierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierServiceImpl implements CarrierService {

    private final CarrierRepository carrierRepository;
    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;

    // ── Efficient lookup via DB index (not full table scan) ──────────────
    private Carrier getCarrierByUsername(String email) {
        return carrierRepository.findByUser_Email(email)
                .orElseThrow(() -> new NoResourceFoundException(
                        "Carrier profile not found. Ensure you are logged in as a Carrier."));
    }

    // ── Ownership guard ───────────────────────────────────────────────────
    private Claim getClaimForCarrier(Long claimId, Carrier carrier) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found: " + claimId));
        if (claim.getCarrier() == null || !claim.getCarrier().getId().equals(carrier.getId())) {
            throw new BadRequestException("Claim #" + claimId + " is not assigned to your carrier account.");
        }
        return claim;
    }

    // ── Guard double-action (already approved / rejected) ────────────────
    private void guardFinalState(Claim claim) {
        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new BadRequestException(
                    "Claim #" + claim.getId() + " is already " + claim.getStatus() + " and cannot be modified.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponse> getAssignedClaims(String username) {
        long start = System.currentTimeMillis();
        Carrier carrier = getCarrierByUsername(username);
        // Use proper DB query — NOT full table scan
        List<Claim> claims = claimRepository.findByCarrier_Id(carrier.getId());
        log.info("Carrier {} fetched {} assigned claims in {}ms",
                carrier.getCompanyName(), claims.size(), System.currentTimeMillis() - start);
        return claimMapper.toDtoList(claims);
    }

    @Transactional
    @Override
    public void validatePolicy(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);
        String note = "[" + LocalDateTime.now() + "] Policy validated by " + carrier.getCompanyName();
        claim.setReviewNotes(claim.getReviewNotes() != null
                ? claim.getReviewNotes() + "\n" + note : note);
        claimRepository.save(claim);
        log.info("Claim {} policy validated by carrier {}", claimId, carrier.getCompanyName());
    }

    @Transactional
    @Override
    public void approveClaim(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);
        guardFinalState(claim);
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(carrier.getCompanyName());
        claim.setReviewedAt(LocalDateTime.now());
        claimRepository.save(claim);
        log.info("Claim {} APPROVED by carrier {}", claimId, carrier.getCompanyName());
    }

    @Transactional
    @Override
    public void rejectClaim(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);
        guardFinalState(claim);
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(carrier.getCompanyName());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setRejectionReason("Rejected by carrier: " + carrier.getCompanyName());
        claimRepository.save(claim);
        log.info("Claim {} REJECTED by carrier {}", claimId, carrier.getCompanyName());
    }

    @Transactional
    @Override
    public void addRemark(Long claimId, String remark, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);
        String entry = "[" + LocalDateTime.now() + "] " + carrier.getCompanyName() + ": " + remark;
        claim.setReviewNotes(claim.getReviewNotes() != null
                ? claim.getReviewNotes() + "\n" + entry : entry);
        claimRepository.save(claim);
        log.info("Remark added to claim {} by carrier {}", claimId, carrier.getCompanyName());
    }

    @Transactional
    @Override
    public void flagSuspicious(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);
        String entry = "SUSPICIOUS — flagged by " + carrier.getCompanyName() + " at " + LocalDateTime.now();
        claim.setRiskFlags(claim.getRiskFlags() != null
                ? claim.getRiskFlags() + " | " + entry : entry);
        claim.setRiskScore(claim.getRiskScore() != null
                ? Math.min(1.0, claim.getRiskScore() + 0.25) : 0.8);
        claimRepository.save(claim);
        log.info("Claim {} flagged suspicious by carrier {}", claimId, carrier.getCompanyName());
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyStatusResponse getPolicyStatus(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim claim = getClaimForCarrier(claimId, carrier);

        boolean hasPolicy = claim.getPolicyNumber() != null
                && !claim.getPolicyNumber().isBlank()
                && !claim.getPolicyNumber().startsWith("TEMP-");
        boolean hasAmount = claim.getAmount() != null && claim.getAmount() > 0;
        boolean notRejected = claim.getStatus() != ClaimStatus.REJECTED;

        String status = (hasPolicy && hasAmount && notRejected) ? "VALID" : "INVALID";
        String reason;
        if ("VALID".equals(status)) {
            reason = "Policy is active and claim details are complete.";
        } else if (!hasPolicy) {
            reason = "Missing or temporary policy number.";
        } else if (!hasAmount) {
            reason = "Claim amount is zero or missing.";
        } else {
            reason = "Claim has been rejected — policy coverage cannot be applied.";
        }

        return PolicyStatusResponse.builder()
                .claimId(claimId)
                .policyNumber(claim.getPolicyNumber())
                .status(status)
                .reason(reason)
                .build();
    }
}
