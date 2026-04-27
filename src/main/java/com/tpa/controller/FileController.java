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
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("claimId") Long claimId,
            @RequestParam(value = "files", required = false) java.util.List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) {
            
        if (files != null && !files.isEmpty()) {
            java.util.List<ClaimDocument> documents = fileUploadService.uploadFiles(claimId, files);
            return ResponseEntity.ok(Map.of(
                    "message", "Files uploaded successfully",
                    "documents", documents
            ));
        } else if (file != null) {
            String type = (documentType != null) ? documentType : "CLAIM_FORM";
            ClaimDocument document = fileUploadService.uploadFile(claimId, type, file);
            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "document", document
            ));
        } else {
            throw new IllegalArgumentException("No files uploaded");
        }
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
            map.put("fileType", doc.getFileType());
            map.put("validationStatus", doc.getValidationStatus());
            map.put("validationIssues", doc.getValidationIssues());
            map.put("confidenceScore", doc.getConfidenceScore());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }
}
