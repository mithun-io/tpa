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
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaim(id));
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
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('FMG_ADMIN', 'FMG_EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<byte[]> exportClaimReport(@PathVariable Long id) {
        byte[] pdfBytes = pdfExportService.exportClaimReport(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claim-report-" + id + ".pdf\"");
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/{id}/audits")
    public ResponseEntity<List<com.tpa.entity.ClaimAudit>> getClaimAudits(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimAudits(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<com.tpa.entity.ClaimAudit>> getClaimTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimAudits(id));
    }
}
