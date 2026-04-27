package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.service.RuleEngineService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    @Override
    public ClaimDecisionResponse evaluateClaim(ClaimDataRequest claimData) {
        List<String> reasons = new ArrayList<>();

        // Rule 1: Claim form missing → Pending
        if (claimData.getClaimFormPresent() == null || !claimData.getClaimFormPresent()) {
            return new ClaimDecisionResponse(ClaimStatus.SUBMITTED, List.of("Claim form is missing"));
        }

        // Rule 2: Combined document missing → Pending
        if (claimData.getCombinedDocumentPresent() == null || !claimData.getCombinedDocumentPresent()) {
            return new ClaimDecisionResponse(ClaimStatus.SUBMITTED, List.of("Combined document is missing"));
        }

        // Rule 3: Policy inactive on admission date → Reject
        if ("INACTIVE".equalsIgnoreCase(claimData.getPolicyStatus())) {
            return new ClaimDecisionResponse(ClaimStatus.REJECTED, List.of("Policy is inactive"));
        }

        // Apply rules that lead to Needs Manual Review
        
        // Rule 4: Policy number missing → Needs Manual Review
        if (claimData.getPolicyNumber() == null || claimData.getPolicyNumber().trim().isEmpty()) {
            reasons.add("Policy number is missing");
        }

        // Rule 5: Patient name mismatch across documents → Needs Manual Review
        if (claimData.getClaimFormPatientName() != null && claimData.getCombinedDocPatientName() != null) {
            if (!claimData.getClaimFormPatientName().trim().equalsIgnoreCase(claimData.getCombinedDocPatientName().trim())) {
                reasons.add("Patient name mismatch across documents");
            }
        } else {
             reasons.add("Patient name missing in one or both documents");
        }

        // Rule 6: Hospital name mismatch across documents → Needs Manual Review
        if (claimData.getClaimFormHospitalName() != null && claimData.getCombinedDocHospitalName() != null) {
            if (!claimData.getClaimFormHospitalName().trim().equalsIgnoreCase(claimData.getCombinedDocHospitalName().trim())) {
                reasons.add("Hospital name mismatch across documents");
            }
        } else {
             reasons.add("Hospital name missing in one or both documents");
        }

        // Rule 7: Admission/discharge date mismatch across documents → Needs Manual Review
        if (claimData.getClaimFormAdmissionDate() != null && claimData.getCombinedDocAdmissionDate() != null) {
            if (!claimData.getClaimFormAdmissionDate().equals(claimData.getCombinedDocAdmissionDate())) {
                reasons.add("Admission date mismatch across documents");
            }
        } else {
             reasons.add("Admission date missing in one or both documents");
        }

        if (claimData.getClaimFormDischargeDate() != null && claimData.getCombinedDocDischargeDate() != null) {
            if (!claimData.getClaimFormDischargeDate().equals(claimData.getCombinedDocDischargeDate())) {
                reasons.add("Discharge date mismatch across documents");
            }
        } else {
             reasons.add("Discharge date missing in one or both documents");
        }

        // Rule 8: Claimed amount > total bill amount → Needs Manual Review
        if (claimData.getClaimedAmount() != null && claimData.getTotalBillAmount() != null) {
            if (claimData.getClaimedAmount() > claimData.getTotalBillAmount()) {
                reasons.add("Claimed amount is greater than total bill amount");
            }
        } else {
             reasons.add("Claimed amount or total bill amount is missing");
        }

        // Rule 9: High claim amount > ₹50,000 → Needs Manual Review
        if (claimData.getClaimedAmount() != null && claimData.getClaimedAmount() > 50000) {
            reasons.add("High claim amount (> ₹50,000)");
        }

        // Rule 10: Possible duplicate claim → Needs Manual Review
        if (claimData.getIsDuplicate() != null && claimData.getIsDuplicate()) {
            reasons.add("Possible duplicate claim detected");
        }

        // Final decision logic: ALL claims must go to REVIEW for admin approval
        if (reasons.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(RuleEngineServiceImpl.class).info("Rule Engine: Claim passed all automated checks. Decision: AI_VALIDATED");
            return new ClaimDecisionResponse(ClaimStatus.AI_VALIDATED, List.of("System auto-verified: Pending admin approval"));
        }
        
        org.slf4j.LoggerFactory.getLogger(RuleEngineServiceImpl.class).warn("Rule Engine: Claim flagged for manual review. Reasons: {}", reasons);
        return new ClaimDecisionResponse(ClaimStatus.UNDER_REVIEW, reasons);
    }
}
