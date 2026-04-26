package com.tpa.controller;

import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.dto.response.ApiResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.dto.response.PolicyStatusResponse;
import com.tpa.service.AiClaimAssistantService;
import com.tpa.service.CarrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/carrier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CARRIER_USER')")
public class CarrierController {

    private final CarrierService carrierService;
    private final AiClaimAssistantService aiClaimAssistantService;

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getAssignedClaims() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Claims fetched", carrierService.getAssignedClaims(currentUser()), 200));
    }

    @PatchMapping("/claims/{id}/validate")
    public ResponseEntity<ApiResponse<Void>> validatePolicy(@PathVariable Long id) {
        carrierService.validatePolicy(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Policy validated", null, 200));
    }

    @PatchMapping("/claims/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveClaim(@PathVariable Long id) {
        carrierService.approveClaim(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Claim approved successfully", null, 200));
    }

    @PatchMapping("/claims/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectClaim(@PathVariable Long id) {
        carrierService.rejectClaim(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Claim rejected successfully", null, 200));
    }

    @PatchMapping("/claims/{id}/remark")
    public ResponseEntity<ApiResponse<Void>> addRemark(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String remark = body.getOrDefault("remark", "");
        if (remark.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Remark cannot be empty", null, 400));
        }
        carrierService.addRemark(id, remark, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Remark added successfully", null, 200));
    }

    @PatchMapping("/claims/{id}/flag")
    public ResponseEntity<ApiResponse<Void>> flagSuspicious(@PathVariable Long id) {
        carrierService.flagSuspicious(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Claim flagged as suspicious", null, 200));
    }

    @GetMapping("/claims/{id}/policy-status")
    public ResponseEntity<ApiResponse<PolicyStatusResponse>> getPolicyStatus(@PathVariable Long id) {
        PolicyStatusResponse result = carrierService.getPolicyStatus(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(true, "Policy status retrieved", result, 200));
    }

    @PostMapping("/claims/{id}/ai-analyze")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> aiAnalyze(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String prompt = (body != null && body.containsKey("prompt"))
                ? body.get("prompt")
                : "Analyze this insurance claim for fraud risk, billing mismatch, and policy coverage issues.";
        AiAnalysisResponse result = aiClaimAssistantService.analyzeClaim(id, prompt);
        return ResponseEntity.ok(new ApiResponse<>(true, "AI analysis complete", result, 200));
    }
}
