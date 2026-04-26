package com.tpa.controller;

import com.tpa.dto.response.FraudDashboardResponse;
import com.tpa.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('FMG_ADMIN')")
    public ResponseEntity<FraudDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(fraudDetectionService.getAdminFraudDashboard());
    }

    @GetMapping("/carrier/dashboard")
    @PreAuthorize("hasRole('CARRIER_USER')")
    public ResponseEntity<FraudDashboardResponse> getCarrierDashboard() {
        return ResponseEntity.ok(fraudDetectionService.getCarrierFraudDashboard(currentUser()));
    }

    @PatchMapping("/admin/claims/{id}/safe")
    @PreAuthorize("hasRole('FMG_ADMIN')")
    public ResponseEntity<Void> markClaimAsSafe(@PathVariable Long id) {
        fraudDetectionService.markClaimAsSafe(id);
        return ResponseEntity.ok().build();
    }
}
