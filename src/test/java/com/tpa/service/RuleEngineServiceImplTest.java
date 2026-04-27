package com.tpa.service;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.service.impl.RuleEngineServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-level tests for RuleEngineServiceImpl from the service package.
 * Detailed edge-case coverage lives in service.impl.RuleEngineServiceImplTest.
 */
@ExtendWith(MockitoExtension.class)
public class RuleEngineServiceImplTest {

    @InjectMocks
    private RuleEngineServiceImpl ruleEngineService;

    private ClaimDataRequest buildFullyValidRequest() {
        return ClaimDataRequest.builder()
                .claimFormPresent(true)
                .combinedDocumentPresent(true)
                .policyNumber("POL-SMOKE-01")
                .policyStatus("ACTIVE")
                .claimedAmount(3000.0)
                .totalBillAmount(4000.0)
                .isDuplicate(false)
                .claimFormPatientName("Test Patient")
                .combinedDocPatientName("Test Patient")
                .claimFormHospitalName("Test Hospital")
                .combinedDocHospitalName("Test Hospital")
                .claimFormAdmissionDate(LocalDate.of(2026, 1, 1))
                .combinedDocAdmissionDate(LocalDate.of(2026, 1, 1))
                .claimFormDischargeDate(LocalDate.of(2026, 1, 5))
                .combinedDocDischargeDate(LocalDate.of(2026, 1, 5))
                .build();
    }

    @Test
    public void testEvaluateClaim_Approved() {
        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(buildFullyValidRequest());

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.AI_VALIDATED);
        assertThat(response.getReasons()).contains("System auto-verified: Pending admin approval");
    }

    @Test
    public void testEvaluateClaim_Rejected_WhenClaimFormMissing() {
        ClaimDataRequest request = buildFullyValidRequest();
        request.setClaimFormPresent(false);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.getReasons()).contains("Claim form is missing");
    }

    @Test
    public void testEvaluateClaim_Review_WhenPolicyNumberMissing() {
        ClaimDataRequest request = buildFullyValidRequest();
        request.setPolicyNumber(null);

        ClaimDecisionResponse response = ruleEngineService.evaluateClaim(request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.getReasons()).contains("Policy number is missing");
    }
}

