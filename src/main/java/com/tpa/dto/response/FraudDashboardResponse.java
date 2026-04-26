package com.tpa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDashboardResponse {
    private DashboardStats stats;
    private List<FraudClaimDto> claims;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private int totalClaims;
        private int flagged;
        private int highRisk;
        private int mediumRisk;
        private int lowRisk;
    }
}
