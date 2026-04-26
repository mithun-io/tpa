package com.tpa.service;

import com.tpa.dto.response.CarrierResponse;
import com.tpa.dto.response.CustomerResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.dto.request.ClaimReviewRequest;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.dto.response.AiAnalysisResponse;

import java.security.Principal;
import java.util.List;

public interface AdminService {

    UserResponse blockUser(Long id);

    UserResponse unblockUser(Long id);

    org.springframework.data.domain.Page<UserResponse> getAllUsers(int page, int size, String search);

    List<CustomerResponse> getAllCustomers();

    ClaimResponse reviewClaim(ClaimReviewRequest request, Principal principal);

    ClaimResponse approveClaim(Long claimId, String reason, Principal principal);

    ClaimResponse rejectClaim(Long claimId, String reason, Principal principal);

    AiAnalysisResponse getClaimAiSummary(Long claimId);

    AiAnalysisResponse askAiAboutClaim(Long claimId, String prompt);

    List<CarrierResponse> getAllCarriers();

    CarrierResponse approveCarrier(Long carrierId);

    CarrierResponse rejectCarrier(Long carrierId);

    ClaimResponse assignCarrierToClaim(Long claimId, Long carrierId);
}