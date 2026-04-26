package com.tpa.controller;

import com.tpa.entity.ClaimDocument;
import com.tpa.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("claimId") Long claimId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
            
        ClaimDocument document = fileUploadService.uploadFile(claimId, documentType, file);
        return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "document", document
        ));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'FMG_EMPLOYEE', 'FMG_ADMIN')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = fileUploadService.downloadFile(id);
        ClaimDocument document = fileUploadService.getDocument(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'FMG_EMPLOYEE', 'FMG_ADMIN')")
    public ResponseEntity<java.util.List<Map<String, Object>>> getDocumentsForClaim(@PathVariable Long claimId) {
        java.util.List<ClaimDocument> documents = fileUploadService.getDocumentsForClaim(claimId);
        java.util.List<Map<String, Object>> response = documents.stream().map(doc -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", doc.getId());
            map.put("fileName", doc.getFileName());
            map.put("type", doc.getType().name());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }
}
