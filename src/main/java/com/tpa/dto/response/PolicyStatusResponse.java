package com.tpa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyStatusResponse {
    private Long claimId;
    private String policyNumber;
    private String status; // "VALID" or "INVALID"
    private String reason;
}
