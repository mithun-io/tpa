package com.tpa.service;

import com.tpa.dto.response.CarrierClaimDetailResponse;
import com.tpa.dto.response.PolicyStatusResponse;

import java.util.List;

public interface CarrierService {
    List<CarrierClaimDetailResponse> getAssignedClaims(String username);
    CarrierClaimDetailResponse       getClaimDetail(Long claimId, String username);
    void validatePolicy(Long claimId, String username);
    void approveClaim(Long claimId, String username);
    void rejectClaim(Long claimId, String username);
    void addRemark(Long claimId, String remark, String username);
    void flagSuspicious(Long claimId, String username);
    PolicyStatusResponse getPolicyStatus(Long claimId, String username);
}
