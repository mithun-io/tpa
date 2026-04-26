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
public class FraudClaimDto {
    private Long claimId;
    private String policyNumber;
    private Double amount;
    private Double riskScore; // 0 to 100
    private String riskLevel; // HIGH, MEDIUM, LOW
    private List<String> reasons;
}
