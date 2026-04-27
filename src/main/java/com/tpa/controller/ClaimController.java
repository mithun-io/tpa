package com.tpa.controller;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.service.ClaimService;
import com.tpa.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PdfExportService pdfExportService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> createClaim(@RequestBody ClaimDataRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(claimService.createClaim(request, username));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CARRIER_USER', 'CUSTOMER')")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ClaimResponse claim = claimService.getClaim(id);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }
        
        // SECURITY: If user is CUSTOMER, ensure they own the claim
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            if (claim.getUserEmail() == null || !claim.getUserEmail().equals(auth.getName())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }
        
        return ResponseEntity.ok(claim);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ClaimResponse>> searchClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String username,
            Pageable pageable) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isCustomer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        // SECURITY: Customers can ONLY search their own claims
        if (isCustomer) {
            username = currentUsername;
        }

        if (!pageable.getSort().isSorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate")
            );
        }
        return ResponseEntity.ok(claimService.searchClaims(status, from, to, minAmount, maxAmount, username, pageable));
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FMG_ADMIN"));
        
        if (isAdmin) {
            return ResponseEntity.ok(claimService.getAllClaims());
        } else {
            // For customers or others, return only their own claims via a filtered search logic
            // (or we can just return forbidden if they shouldn't use the bulk list)
            return ResponseEntity.ok(claimService.searchClaims(null, null, null, null, null, auth.getName(), Pageable.unpaged()).getContent());
        }
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CARRIER_USER', 'CUSTOMER')")
    public ResponseEntity<byte[]> exportClaimReport(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ClaimResponse claim = claimService.getClaim(id);

        // SECURITY: If user is CUSTOMER, ensure they own the claim
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            if (claim.getUserEmail() == null || !claim.getUserEmail().equals(auth.getName())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }

        byte[] pdfBytes = pdfExportService.exportClaimReport(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claim-report-" + id + ".pdf\"");
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/{id}/audits")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CARRIER_USER', 'CUSTOMER')")
    public ResponseEntity<List<com.tpa.entity.ClaimAudit>> getClaimAudits(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ClaimResponse claim = claimService.getClaim(id);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            if (claim.getUserEmail() == null || !claim.getUserEmail().equals(auth.getName())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(claimService.getClaimAudits(id));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CARRIER_USER', 'CUSTOMER')")
    public ResponseEntity<List<com.tpa.entity.ClaimAudit>> getClaimTimeline(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ClaimResponse claim = claimService.getClaim(id);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            if (claim.getUserEmail() == null || !claim.getUserEmail().equals(auth.getName())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(claimService.getClaimAudits(id));
    }
}
