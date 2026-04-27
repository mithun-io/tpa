package com.tpa.controller;

import com.tpa.dto.request.ClaimReviewRequest;
import com.tpa.dto.response.CarrierResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.dto.response.CustomerResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FMG_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final com.tpa.service.ClaimService claimService;

    @GetMapping("/users")
    public ResponseEntity<org.springframework.data.domain.Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size, search));
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<UserResponse> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.blockUser(id));
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<UserResponse> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unblockUser(id));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    @GetMapping("/claims")
    public ResponseEntity<org.springframework.data.domain.Page<ClaimResponse>> getAllClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) com.tpa.enums.ClaimStatus status) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdDate").descending());
        return ResponseEntity.ok(claimService.searchClaims(status, null, null, null, null, null, pageable));
    }

    @PatchMapping("/claims/review")
    public ResponseEntity<ClaimResponse> reviewClaim(@Valid @RequestBody ClaimReviewRequest request, Principal principal) {
        return ResponseEntity.ok(adminService.reviewClaim(request, principal));
    }

    @PostMapping("/claims/review")
    public ResponseEntity<ClaimResponse> reviewClaimPost(@Valid @RequestBody ClaimReviewRequest request, Principal principal) {
        return ResponseEntity.ok(adminService.reviewClaim(request, principal));
    }

    @PutMapping("/claims/{id}/approve")
    public ResponseEntity<ClaimResponse> approveClaim(@PathVariable Long id, @RequestParam(required = false) String reason, Principal principal) {
        return ResponseEntity.ok(adminService.approveClaim(id, reason != null ? reason : "Approved by admin", principal));
    }

    @PutMapping("/claims/{id}/reject")
    public ResponseEntity<ClaimResponse> rejectClaim(@PathVariable Long id, @RequestParam String reason, Principal principal) {
        return ResponseEntity.ok(adminService.rejectClaim(id, reason, principal));
    }

    @GetMapping("/claims/{id}/ai-summary")
    public ResponseEntity<AiAnalysisResponse> getClaimAiSummary(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getClaimAiSummary(id));
    }

    @PostMapping("/claims/{id}/ai-chat")
    public ResponseEntity<AiAnalysisResponse> askAiAboutClaim(@PathVariable Long id, @RequestBody java.util.Map<String, String> request) {
        String prompt = request.getOrDefault("prompt", "Analyze this claim");
        return ResponseEntity.ok(adminService.askAiAboutClaim(id, prompt));
    }

    @GetMapping("/monitoring")
    public ResponseEntity<java.util.Map<String, Object>> getSystemMonitoring() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // Mock Kafka status
        response.put("kafka", java.util.Map.of("status", "ONLINE", "brokers", "localhost:9092", "topics", List.of("claim_events", "notifications")));
        
        // Failed claims
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("createdDate").descending());
        org.springframework.data.domain.Page<ClaimResponse> failedClaims = claimService.searchClaims(com.tpa.enums.ClaimStatus.REJECTED, null, null, null, null, null, pageable);
        response.put("failedClaims", failedClaims.getContent());
        
        // Mock Error logs
        response.put("errorLogs", List.of(
            java.util.Map.of("timestamp", java.time.LocalDateTime.now().minusHours(1), "level", "ERROR", "message", "Failed to connect to AI provider"),
            java.util.Map.of("timestamp", java.time.LocalDateTime.now().minusHours(3), "level", "WARN", "message", "Rate limit exceeded on AI provider API"),
            java.util.Map.of("timestamp", java.time.LocalDateTime.now().minusDays(1), "level", "ERROR", "message", "NullPointerException in RuleEngine")
        ));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carriers")
    public ResponseEntity<List<CarrierResponse>> getAllCarriers() {
        return ResponseEntity.ok(adminService.getAllCarriers());
    }

    @PatchMapping("/carriers/{id}/approve")
    public ResponseEntity<CarrierResponse> approveCarrier(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveCarrier(id));
    }

    @PatchMapping("/carriers/{id}/reject")
    public ResponseEntity<CarrierResponse> rejectCarrier(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectCarrier(id));
    }

    @PatchMapping("/claims/{id}/assign-carrier")
    public ResponseEntity<ClaimResponse> assignCarrier(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        Long carrierId = body.get("carrierId");
        if (carrierId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adminService.assignCarrierToClaim(id, carrierId));
    }
}
