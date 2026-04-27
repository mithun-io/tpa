package com.tpa.service;

import com.tpa.entity.ClaimDocument;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {
    ClaimDocument uploadFile(Long claimId, String documentType, MultipartFile file);
    List<ClaimDocument> uploadFiles(Long claimId, List<MultipartFile> files);
    Resource downloadFile(Long documentId);
    ClaimDocument getDocument(Long documentId);
    java.util.List<ClaimDocument> getDocumentsForClaim(Long claimId);
}
