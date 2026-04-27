package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.entity.Claim;
import com.tpa.entity.ClaimDocument;
import com.tpa.enums.DocumentType;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.repository.ClaimDocumentRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.service.ClaimService;
import com.tpa.service.FileUploadService;
import com.tpa.service.RuleEngineService;
import com.tpa.service.StorageProvider;
import com.tpa.service.AiClaimAssistantService;
import com.tpa.dto.response.DocumentValidationResponse;
import com.tpa.enums.ClaimStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final ClaimDocumentRepository claimDocumentRepository;
    private final ClaimRepository claimRepository;
    private final StorageProvider storageProvider;
    private final ClaimEventProducer claimEventProducer;
    private final RuleEngineService ruleEngineService;
    private final ClaimService claimService;
    private final AiClaimAssistantService aiClaimAssistantService;
    private final ObjectMapper objectMapper;

    @Override
    public ClaimDocument uploadFile(Long claimId, String documentType, MultipartFile file) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found"));

        String filePath = storageProvider.storeFile(file);
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");

        ClaimDocument document = ClaimDocument.builder()
                .claim(claim)
                .fileName(originalFileName)
                .filePath(filePath)
                .type(DocumentType.valueOf(documentType.toUpperCase()))
                .fileType(file.getContentType() != null && file.getContentType().contains("pdf") ? "PDF" : "IMAGE")
                .build();

        try {
            DocumentValidationResponse validationResponse = aiClaimAssistantService.validateDocument(file, documentType);
            document.setValidationStatus(validationResponse.getStatus());
            document.setConfidenceScore(validationResponse.getConfidenceScore());
            document.setValidationIssues(objectMapper.writeValueAsString(validationResponse.getIssues()));

            if ("INVALID".equalsIgnoreCase(validationResponse.getStatus())) {
                claim.setStatus(ClaimStatus.UNDER_REVIEW);
                claimRepository.save(claim);
            }
        } catch (Exception e) {
            log.error("AI validation during upload failed", e);
        }

        ClaimDocument saved = claimDocumentRepository.save(document);
        log.info("Document [{}] uploaded for claim {}", documentType, claimId);

        // Check if BOTH documents are now uploaded → trigger processing
        List<ClaimDocument> docs = claimDocumentRepository.findByClaim(claim);
        boolean hasClaimForm = docs.stream().anyMatch(d -> d.getType() == DocumentType.CLAIM_FORM);
        boolean hasCombinedDoc = docs.stream().anyMatch(d -> d.getType() == DocumentType.COMBINED_DOCUMENT);

        if (hasClaimForm && hasCombinedDoc) {
            log.info("Both documents uploaded for claim {}. Running rule engine synchronously...", claimId);

            // Use actual data from the claim entity
            ClaimDataRequest request = ClaimDataRequest.builder()
                    .claimFormPresent(true)
                    .combinedDocumentPresent(true)
                    .policyNumber(claim.getPolicyNumber())
                    .policyStatus("ACTIVE")
                    .claimedAmount(claim.getAmount())
                    .isDuplicate(false)
                    .claimFormPatientName(claim.getPatientName())
                    .combinedDocPatientName(claim.getPatientName())
                    .claimFormHospitalName(claim.getHospitalName())
                    .combinedDocHospitalName(claim.getHospitalName())
                    .claimFormAdmissionDate(claim.getAdmissionDate())
                    .combinedDocAdmissionDate(claim.getAdmissionDate())
                    .claimFormDischargeDate(claim.getDischargeDate())
                    .combinedDocDischargeDate(claim.getDischargeDate())
                    .totalBillAmount(claim.getTotalBillAmount())
                    .policyId(claim.getPolicyId())
                    .carrierName(claim.getCarrierName())
                    .policyName(claim.getPolicyName())
                    .claimType(claim.getClaimType())
                    .diagnosis(claim.getDiagnosis())
                    .billNumber(claim.getBillNumber())
                    .billDate(claim.getBillDate())
                    .build();

            // Always process synchronously — guarantees DB update before response returns
            try {
                var decision = ruleEngineService.evaluateClaim(request);
                claimService.processClaimDecision(claimId, decision);
                log.info("Claim {} processed synchronously. Final status: {}", claimId, decision.getStatus());
            } catch (Exception e) {
                log.error("Rule engine processing failed for claim {}: {}", claimId, e.getMessage(), e);
            }

            // Publish to Kafka async for downstream notifications (non-critical)
            try {
                claimEventProducer.publishClaimCreatedEvent(claimId, ClaimDataRequest.builder()
                        .claimFormPresent(true)
                        .combinedDocumentPresent(true)
                        .policyNumber(claim.getPolicyNumber())
                        .policyStatus("ACTIVE")
                        .claimedAmount(claim.getAmount())
                        .isDuplicate(false)
                        .build());
                log.info("Kafka notification published for claim {}", claimId);
            } catch (Exception e) {
                log.warn("Kafka publish failed for claim {} (non-critical): {}", claimId, e.getMessage());
            }
        } else {
            log.info("Claim {} — waiting for {} document.", claimId,
                    !hasClaimForm ? "CLAIM_FORM" : "COMBINED_DOCUMENT");
        }

        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<ClaimDocument> uploadFiles(Long claimId, List<MultipartFile> files) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found"));

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files uploaded");
        }

        // 1. Validate: At least one PDF is mandatory
        boolean hasPdf = files.stream()
                .anyMatch(file -> file.getContentType() != null && file.getContentType().equals("application/pdf"));

        if (!hasPdf) {
            throw new IllegalArgumentException("At least one PDF document is required");
        }

        // 2. Validate: Allowed types (PDF, JPG, PNG)
        List<String> allowedTypes = List.of("application/pdf", "image/jpeg", "image/png");
        files.forEach(file -> {
            if (!allowedTypes.contains(file.getContentType())) {
                throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
            }
        });

        java.util.ArrayList<ClaimDocument> savedDocuments = new java.util.ArrayList<>();
        boolean claimFormAssigned = false;

        for (MultipartFile file : files) {
            String filePath = storageProvider.storeFile(file);
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
            String contentType = file.getContentType();
            
            DocumentType docType;
            String fileCategory;

            if (contentType != null && contentType.equals("application/pdf")) {
                fileCategory = "PDF";
                if (!claimFormAssigned) {
                    docType = DocumentType.CLAIM_FORM;
                    claimFormAssigned = true;
                } else {
                    docType = DocumentType.COMBINED_DOCUMENT;
                }
            } else {
                fileCategory = "IMAGE";
                docType = DocumentType.SUPPORTING_DOCUMENT;
            }

            ClaimDocument document = ClaimDocument.builder()
                    .claim(claim)
                    .fileName(originalFileName)
                    .filePath(filePath)
                    .type(docType)
                    .fileType(fileCategory)
                    .build();

            // AI Validation
            try {
                log.info("Running AI validation for document: {} (Type: {})", originalFileName, docType);
                DocumentValidationResponse validationResponse = aiClaimAssistantService.validateDocument(file, docType.name());
                document.setValidationStatus(validationResponse.getStatus());
                document.setConfidenceScore(validationResponse.getConfidenceScore());
                document.setValidationIssues(objectMapper.writeValueAsString(validationResponse.getIssues()));

                if ("INVALID".equalsIgnoreCase(validationResponse.getStatus())) {
                    log.warn("AI Validation flagged document {} as INVALID for claim {}", originalFileName, claimId);
                    claim.setStatus(ClaimStatus.UNDER_REVIEW);
                } else {
                    log.info("AI Validation passed for document {} (Score: {})", originalFileName, validationResponse.getConfidenceScore());
                }
            } catch (Exception e) {
                log.error("NON-CRITICAL: AI validation failed for file {}. Continuing with default state.", originalFileName, e);
                document.setValidationStatus("UNKNOWN");
            }

            savedDocuments.add(claimDocumentRepository.save(document));
        }

        // Persist any status updates from validation (e.g., moved to UNDER_REVIEW)
        claimRepository.save(claim);
        log.info("Successfully uploaded {} documents for claim {}", savedDocuments.size(), claimId);

        // Trigger processing synchronously if we have a PDF
        triggerRuleEngine(claim);

        return savedDocuments;
    }

    private void triggerRuleEngine(Claim claim) {
        log.info("Triggering rule engine for claim {}", claim.getId());
        
        ClaimDataRequest request = ClaimDataRequest.builder()
                .claimFormPresent(true)
                .combinedDocumentPresent(true)
                .policyNumber(claim.getPolicyNumber())
                .policyStatus("ACTIVE")
                .claimedAmount(claim.getAmount())
                .isDuplicate(false)
                .claimFormPatientName(claim.getPatientName())
                .combinedDocPatientName(claim.getPatientName())
                .claimFormHospitalName(claim.getHospitalName())
                .combinedDocHospitalName(claim.getHospitalName())
                .claimFormAdmissionDate(claim.getAdmissionDate())
                .combinedDocAdmissionDate(claim.getAdmissionDate())
                .claimFormDischargeDate(claim.getDischargeDate())
                .combinedDocDischargeDate(claim.getDischargeDate())
                .totalBillAmount(claim.getTotalBillAmount())
                .policyId(claim.getPolicyId())
                .carrierName(claim.getCarrierName())
                .policyName(claim.getPolicyName())
                .claimType(claim.getClaimType())
                .diagnosis(claim.getDiagnosis())
                .billNumber(claim.getBillNumber())
                .billDate(claim.getBillDate())
                .build();

        try {
            var decision = ruleEngineService.evaluateClaim(request);
            claimService.processClaimDecision(claim.getId(), decision);
            log.info("Claim {} processed synchronously. Final status: {}", claim.getId(), decision.getStatus());
            
            // Publish to Kafka
            claimEventProducer.publishClaimCreatedEvent(claim.getId(), request);
        } catch (Exception e) {
            log.error("Processing failed for claim {}: {}", claim.getId(), e.getMessage());
        }
    }

    @Override
    public Resource downloadFile(Long documentId) {
        ClaimDocument document = claimDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NoResourceFoundException("Document not found"));

        return storageProvider.loadFileAsResource(document.getFilePath());
    }

    @Override
    public ClaimDocument getDocument(Long documentId) {
        return claimDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NoResourceFoundException("Document not found"));
    }

    @Override
    public List<ClaimDocument> getDocumentsForClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("Claim not found"));
        return claimDocumentRepository.findByClaim(claim);
    }
}
