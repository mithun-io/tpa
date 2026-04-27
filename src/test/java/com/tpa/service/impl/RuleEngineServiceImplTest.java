package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.enums.ClaimStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceImplTest {

    @InjectMocks
    private RuleEngineServiceImpl ruleEngineService;

    // ========== Helpers ==========

    private ClaimDataRequest buildValidRequest() {
        return ClaimDataRequest.builder()
                .claimFormPresent(true)
                .combinedDocumentPresent(true)
                .policyNumber("POL-001")
                .policyStatus("ACTIVE")
                .claimedAmount(5000.0)
                .totalBillAmount(6000.0)
                .isDuplicate(false)
                .claimFormPatientName("John Doe")
                .combinedDocPatientName("John Doe")
                .claimFormHospitalName("Apollo Hospitals")
                .combinedDocHospitalName("Apollo Hospitals")
                .claimFormAdmissionDate(LocalDate.of(2026, 1, 1))
                .combinedDocAdmissionDate(LocalDate.of(2026, 1, 1))
                .claimFormDischargeDate(LocalDate.of(2026, 1, 5))
                .combinedDocDischargeDate(LocalDate.of(2026, 1, 5))
                .build();
    }

    // ========== Positive Cases ==========

    @Test
    void evaluateClaim_shouldReturnAiValidated_whenAllRulesPass() {
        ClaimDataRequest request = buildValidRequest();

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.AI_VALIDATED);
        assertThat(response.getReasons()).contains("System auto-verified: Pending admin approval");
    }

    // ========== Negative Cases — Hard Reject ==========

    @Test
    void evaluateClaim_shouldReturnPending_whenClaimFormMissing() {
        ClaimDataRequest request = buildValidRequest();
        request.setClaimFormPresent(false);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.getReasons()).contains("Claim form is missing");
    }

    @Test
    void evaluateClaim_shouldReturnPending_whenCombinedDocumentMissing() {
        ClaimDataRequest request = buildValidRequest();
        request.setCombinedDocumentPresent(false);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.getReasons()).contains("Combined document is missing");
    }

    @Test
    void evaluateClaim_shouldReturnRejected_whenPolicyIsInactive() {
        ClaimDataRequest request = buildValidRequest();
        request.setPolicyStatus("INACTIVE");

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.getReasons()).contains("Policy is inactive");
    }

    // ========== Review Cases ==========

    @Test
    void evaluateClaim_shouldReturnReview_whenPatientNameMismatch() {
        ClaimDataRequest request = buildValidRequest();
        request.setCombinedDocPatientName("Jane Doe"); // mismatch

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Patient name mismatch across documents");
    }

    @Test
    void evaluateClaim_shouldReturnReview_whenHospitalNameMismatch() {
        ClaimDataRequest request = buildValidRequest();
        request.setCombinedDocHospitalName("City Hospital"); // mismatch

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Hospital name mismatch across documents");
    }

    @Test
    void evaluateClaim_shouldReturnReview_whenAdmissionDateMismatch() {
        ClaimDataRequest request = buildValidRequest();
        request.setCombinedDocAdmissionDate(LocalDate.of(2026, 1, 2)); // mismatch

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Admission date mismatch across documents");
    }

    @Test
    void evaluateClaim_shouldReturnReview_whenClaimedAmountExceedsBillAmount() {
        ClaimDataRequest request = buildValidRequest();
        request.setClaimedAmount(10000.0);
        request.setTotalBillAmount(5000.0);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Claimed amount is greater than total bill amount");
    }

    @Test
    void evaluateClaim_shouldReturnReview_whenClaimAmountIsAbove50000() {
        ClaimDataRequest request = buildValidRequest();
        request.setClaimedAmount(55000.0);
        request.setTotalBillAmount(60000.0);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("High claim amount (> ₹50,000)");
    }

    // ========== Edge Cases ==========

    @Test
    void evaluateClaim_shouldReturnReview_whenDuplicateClaimDetected() {
        ClaimDataRequest request = buildValidRequest();
        request.setIsDuplicate(true);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Possible duplicate claim detected");
    }

    @Test
    void evaluateClaim_shouldReturnReview_whenPolicyNumberIsMissing() {
        ClaimDataRequest request = buildValidRequest();
        request.setPolicyNumber(null);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Policy number is missing");
    }

    @Test
    void evaluateClaim_shouldAccumulateMultipleReasons_whenSeveralRulesViolated() {
        ClaimDataRequest request = buildValidRequest();
        request.setIsDuplicate(true);
        request.setCombinedDocPatientName("Wrong Name");
        request.setCombinedDocHospitalName("Wrong Hospital");

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(response.getReasons()).contains(
                "Possible duplicate claim detected",
                "Patient name mismatch across documents",
                "Hospital name mismatch across documents"
        );
    }
}
