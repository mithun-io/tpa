package com.tpa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rich carrier-facing claim detail DTO.
 * Contains full patient + claim + fraud + policy info for carrier use.
 * Returned by GET /api/v1/carrier/claims and GET /api/v1/carrier/claims/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierClaimDetailResponse {

    // ── Claim core ──────────────────────────────────────────────────────────
    private Long   claimId;
    private String policyNumber;
    private String status;
    private Double amount;
    private Double totalBillAmount;
    private String claimType;
    private String diagnosis;
    private String hospitalName;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private LocalDateTime createdDate;
    private LocalDateTime processedDate;
    private String rejectionReason;
    private String reviewNotes;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    // ── Nested sections ─────────────────────────────────────────────────────
    private PatientInfo patient;
    private FraudInfo   fraud;
    private PolicyInfo  policy;

    // ────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private String    name;
        private String    email;
        private String    mobile;
        private LocalDate dateOfBirth;
        private String    gender;
        private String    address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudInfo {
        /** 0–100 scale (from FraudDetectionService) */
        private Double  riskScore;
        private String  riskLevel;   // LOW / MEDIUM / HIGH
        private Integer healthScore; // 0–100 (100 = healthy)
        private String  riskFlags;
        private String  aiSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyInfo {
        private String policyNumber;
        private String status;  // VALID | INVALID
        private String reason;
    }
}
