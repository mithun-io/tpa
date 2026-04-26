package com.tpa.controller;

import com.tpa.service.AiClaimAssistantService;
import com.tpa.dto.request.AiValidationRequest;
import com.tpa.dto.response.AiAnalysisResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiClaimAssistantService aiClaimAssistantService;

    @PostMapping("/analyze/{claimId}")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<AiAnalysisResponse> analyzeClaim(
            @PathVariable Long claimId,
            @RequestBody(required = false) Map<String, String> request) {
        
        String prompt = "Summarize this claim and highlight any issues.";
        if (request != null && request.containsKey("prompt")) {
            prompt = request.get("prompt");
        }
        
        return ResponseEntity.ok(aiClaimAssistantService.analyzeClaim(claimId, prompt));
    }

    @PostMapping("/claims/{id}/generate-summary")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'CARRIER_USER', 'CUSTOMER')")
    public ResponseEntity<Map<String, String>> generateClaimSummary(@PathVariable Long id) {
        String summary = aiClaimAssistantService.generateClaimSummary(id);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @PostMapping("/validate-claim")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiAnalysisResponse> validateClaim(@Valid @RequestBody AiValidationRequest request) {
        return ResponseEntity.ok(aiClaimAssistantService.validatePreClaim(request));
    }

    @PostMapping(value = "/validate-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.tpa.dto.response.DocumentValidationResponse> validateDocument(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("documentType") String documentType) {
        return ResponseEntity.ok(aiClaimAssistantService.validateDocument(file, documentType));
    }
}
