package com.tpa.service;

import com.tpa.dto.request.AiValidationRequest;
import com.tpa.dto.response.AiAnalysisResponse;

public interface AiClaimAssistantService {
    AiAnalysisResponse analyzeClaim(Long claimId, String prompt);
    String generateClaimSummary(Long claimId);
    AiAnalysisResponse validatePreClaim(AiValidationRequest request);
    com.tpa.dto.response.DocumentValidationResponse validateDocument(org.springframework.web.multipart.MultipartFile file, String documentType);
}
