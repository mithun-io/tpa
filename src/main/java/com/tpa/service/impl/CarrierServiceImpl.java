package com.tpa.service.impl;

import com.tpa.dto.response.CarrierClaimDetailResponse;
import com.tpa.dto.response.CarrierClaimDetailResponse.FraudInfo;
import com.tpa.dto.response.CarrierClaimDetailResponse.PatientInfo;
import com.tpa.dto.response.CarrierClaimDetailResponse.PolicyInfo;
import com.tpa.dto.response.PolicyStatusResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.BadRequestException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.event.ClaimNotificationEvent;
import com.tpa.kafka.producer.ProducerService;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.service.CarrierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierServiceImpl implements CarrierService {

    private final CarrierRepository  carrierRepository;
    private final ClaimRepository    claimRepository;
    private final ProducerService    producerService;
    private final com.tpa.service.NotificationService notificationService;
    private final com.tpa.service.ClaimStateMachine claimStateMachine;
    private final com.tpa.service.AuditLogService auditLogService;

    // ── Lookups ───────────────────────────────────────────────────────────
    private Carrier getCarrierByUsername(String email) {
        return carrierRepository.findByUser_Email(email)
                .orElseThrow(() -> new NoResourceFoundException(
                        "Carrier profile not found. Ensure you are logged in as a Carrier."));
    }

    private Claim getClaimForCarrier(Long claimId, Carrier carrier) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found: " + claimId));
        if (claim.getCarrier() == null || !claim.getCarrier().getId().equals(carrier.getId())) {
            throw new BadRequestException("Claim #" + claimId + " is not assigned to your carrier account.");
        }
        return claim;
    }

    private void guardFinalState(Claim claim) {
        if (claim.getStatus() == ClaimStatus.CARRIER_APPROVED || claim.getStatus() == ClaimStatus.REJECTED || claim.getStatus() == ClaimStatus.SETTLED) {
            throw new BadRequestException(
                    "Claim #" + claim.getId() + " is already " + claim.getStatus() + " and cannot be modified.");
        }
    }

    // ── Mapping helper ────────────────────────────────────────────────────
    private CarrierClaimDetailResponse toDetail(Claim c) {
        User u = c.getUser();

        PatientInfo patient = (u == null) ? null : PatientInfo.builder()
                .name(u.getUsername())
                .email(u.getEmail())
                .mobile(u.getMobile())
                .dateOfBirth(u.getDateOfBirth())
                .gender(u.getGender() != null ? u.getGender().name() : null)
                .address(u.getAddress())
                .build();

        FraudInfo fraud = FraudInfo.builder()
                .riskScore(c.getRiskScore())
                .riskLevel(c.getRiskLevel())
                .healthScore(c.getHealthScore())
                .riskFlags(c.getRiskFlags())
                .aiSummary(c.getAiSummary())
                .build();

        // Inline policy validity check
        boolean hasPolicy  = c.getPolicyNumber() != null && !c.getPolicyNumber().isBlank()
                             && !c.getPolicyNumber().startsWith("TEMP-");
        boolean hasAmount  = c.getAmount() != null && c.getAmount() > 0;
        boolean notRejected = c.getStatus() != ClaimStatus.REJECTED;
        String  polStatus  = (hasPolicy && hasAmount && notRejected) ? "VALID" : "INVALID";
        String  polReason  = "VALID".equals(polStatus)
                ? "Policy is active and claim details are complete."
                : !hasPolicy ? "Missing or temporary policy number."
                : !hasAmount ? "Claim amount is zero or missing."
                : "Claim has been rejected — policy coverage cannot be applied.";

        PolicyInfo policy = PolicyInfo.builder()
                .policyNumber(c.getPolicyNumber())
                .status(polStatus)
                .reason(polReason)
                .build();

        return CarrierClaimDetailResponse.builder()
                .claimId(c.getId())
                .policyNumber(c.getPolicyNumber())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .amount(c.getAmount())
                .totalBillAmount(c.getTotalBillAmount())
                .claimType(c.getClaimType())
                .diagnosis(c.getDiagnosis())
                .hospitalName(c.getHospitalName())
                .admissionDate(c.getAdmissionDate())
                .dischargeDate(c.getDischargeDate())
                .createdDate(c.getCreatedDate())
                .processedDate(c.getProcessedDate())
                .rejectionReason(c.getRejectionReason())
                .reviewNotes(c.getReviewNotes())
                .reviewedBy(c.getReviewedBy())
                .reviewedAt(c.getReviewedAt())
                .patient(patient)
                .fraud(fraud)
                .policy(policy)
                .build();
    }

    // ── Public API ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CarrierClaimDetailResponse> getAssignedClaims(String username) {
        Carrier carrier = getCarrierByUsername(username);
        List<Claim> claims = claimRepository.findByCarrier_Id(carrier.getId());
        log.info("Carrier {} fetched {} assigned claims", carrier.getCompanyName(), claims.size());
        return claims.stream().map(this::toDetail).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarrierClaimDetailResponse getClaimDetail(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        return toDetail(claim);
    }

    @Override
    @Transactional
    public void validatePolicy(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        String  note    = "[" + LocalDateTime.now() + "] Policy validated by " + carrier.getCompanyName();
        claim.setReviewNotes(claim.getReviewNotes() != null ? claim.getReviewNotes() + "\n" + note : note);
        claimRepository.save(claim);
        log.info("Claim {} policy validated by carrier {}", claimId, carrier.getCompanyName());
    }

    @Override
    @Transactional
    public void approveClaim(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        claimStateMachine.validateTransition(claim.getStatus(), ClaimStatus.CARRIER_APPROVED);
        ClaimStatus previousStatus = claim.getStatus();
        
        claim.setStatus(ClaimStatus.CARRIER_APPROVED);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(carrier.getCompanyName());
        claim.setReviewedAt(LocalDateTime.now());
        claimRepository.save(claim);
        
        auditLogService.logAction(claimId, "CARRIER_APPROVAL", previousStatus, ClaimStatus.CARRIER_APPROVED);
        log.info("Claim {} CARRIER_APPROVED by carrier {}", claimId, carrier.getCompanyName());
        
        producerService.sendClaimNotificationEvent(ClaimNotificationEvent.builder()
                .claimId(claim.getId()).policyNumber(claim.getPolicyNumber())
                .customerEmail(claim.getUser().getEmail()).status(ClaimStatus.CARRIER_APPROVED)
                .message("Your claim has been APPROVED by the carrier.").build());
        // Notify all admins asynchronously
        notificationService.notifyAllAdmins(
                "\uD83D\uDCCB Claim #" + claimId + " Approved by Carrier",
                "Claim #" + claimId + " (Policy: " + claim.getPolicyNumber() + ") has been approved by carrier " + carrier.getCompanyName() + ". Payment can now be released.",
                "/claims/" + claimId
        );
    }

    @Override
    @Transactional
    public void rejectClaim(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        guardFinalState(claim);
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(carrier.getCompanyName());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setRejectionReason("Rejected by carrier: " + carrier.getCompanyName());
        claimRepository.save(claim);
        log.info("Claim {} REJECTED by carrier {}", claimId, carrier.getCompanyName());
        producerService.sendClaimNotificationEvent(ClaimNotificationEvent.builder()
                .claimId(claim.getId()).policyNumber(claim.getPolicyNumber())
                .customerEmail(claim.getUser().getEmail()).status(ClaimStatus.REJECTED)
                .message("Your claim has been REJECTED by the carrier.").build());
        notificationService.notifyAllAdmins(
                "❌ Claim #" + claimId + " Rejected by Carrier",
                "Claim #" + claimId + " (Policy: " + claim.getPolicyNumber() + ") has been rejected by carrier " + carrier.getCompanyName() + ".",
                "/claims/" + claimId
        );
    }

    @Override
    @Transactional
    public void addRemark(Long claimId, String remark, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        String  entry   = "[" + LocalDateTime.now() + "] " + carrier.getCompanyName() + ": " + remark;
        claim.setReviewNotes(claim.getReviewNotes() != null ? claim.getReviewNotes() + "\n" + entry : entry);
        claimRepository.save(claim);
        log.info("Remark added to claim {} by carrier {}", claimId, carrier.getCompanyName());
    }

    @Override
    @Transactional
    public void flagSuspicious(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        String  entry   = "SUSPICIOUS — flagged by " + carrier.getCompanyName() + " at " + LocalDateTime.now();
        claim.setRiskFlags(claim.getRiskFlags() != null ? claim.getRiskFlags() + " | " + entry : entry);
        // riskScore is 0-100 scale — add 25 points, cap at 100
        claim.setRiskScore(claim.getRiskScore() != null
                ? Math.min(100.0, claim.getRiskScore() + 25.0) : 75.0);
        claimRepository.save(claim);
        log.info("Claim {} flagged suspicious by carrier {}", claimId, carrier.getCompanyName());
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyStatusResponse getPolicyStatus(Long claimId, String username) {
        Carrier carrier = getCarrierByUsername(username);
        Claim   claim   = getClaimForCarrier(claimId, carrier);
        boolean hasPolicy  = claim.getPolicyNumber() != null && !claim.getPolicyNumber().isBlank()
                             && !claim.getPolicyNumber().startsWith("TEMP-");
        boolean hasAmount  = claim.getAmount() != null && claim.getAmount() > 0;
        boolean notRejected = claim.getStatus() != ClaimStatus.REJECTED;
        String  status     = (hasPolicy && hasAmount && notRejected) ? "VALID" : "INVALID";
        String  reason     = "VALID".equals(status) ? "Policy is active and claim details are complete."
                : !hasPolicy ? "Missing or temporary policy number."
                : !hasAmount ? "Claim amount is zero or missing."
                : "Claim has been rejected — policy coverage cannot be applied.";
        return PolicyStatusResponse.builder()
                .claimId(claimId).policyNumber(claim.getPolicyNumber())
                .status(status).reason(reason).build();
    }
}
