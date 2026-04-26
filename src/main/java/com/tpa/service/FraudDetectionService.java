package com.tpa.service;

import com.tpa.dto.response.FraudDashboardResponse;
import com.tpa.entity.Claim;

public interface FraudDetectionService {
    void calculateAndSaveHealthAndRisk(Claim claim);
    FraudDashboardResponse getAdminFraudDashboard();
    FraudDashboardResponse getCarrierFraudDashboard(String carrierUsername);
    void markClaimAsSafe(Long claimId);
}
