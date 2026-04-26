package com.tpa.dto.response;

import com.tpa.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
    private Long id;
    private String policyNumber;
    private ClaimStatus status;
    private Double amount;
    private LocalDateTime createdDate;
    private LocalDateTime processedDate;
    private String rejectionReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    // AI Fraud Detection & Health fields
    private Double riskScore;
    private String riskFlags;
    private Integer healthScore;
    private String riskLevel;
    private String aiSummary;

    // Patient / hospital details
    private String patientName;
    private String hospitalName;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private Double totalBillAmount;
    private String diagnosis;
    private String claimType;

    // Submitter info
    private String username;
}
