package com.tpa.service.impl;

import com.tpa.entity.Claim;
import com.tpa.entity.ClaimDocument;
import com.tpa.enums.DocumentType;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.repository.ClaimDocumentRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.service.ClaimService;
import com.tpa.service.RuleEngineService;
import com.tpa.service.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {

    @Mock
    private ClaimDocumentRepository claimDocumentRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private ClaimEventProducer claimEventProducer;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private ClaimService claimService;

    @InjectMocks
    private FileUploadServiceImpl fileUploadService;

    @Test
    void uploadFile_shouldSaveDocument_whenFileIsValid() {
        Long claimId = 1L;
        Claim claim = Claim.builder().id(claimId).build();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(storageProvider.storeFile(file)).thenReturn("/path/to/test.pdf");
        
        ClaimDocument savedDoc = ClaimDocument.builder().id(100L).build();
        when(claimDocumentRepository.save(any(ClaimDocument.class))).thenReturn(savedDoc);
        
        when(claimDocumentRepository.findByClaim(claim)).thenReturn(List.of());

        ClaimDocument result = fileUploadService.uploadFile(claimId, "CLAIM_FORM", file);

        assertThat(result.getId()).isEqualTo(100L);
        verify(storageProvider, times(1)).storeFile(file);
        verify(claimDocumentRepository, times(1)).save(any(ClaimDocument.class));
    }

    @Test
    void uploadFile_shouldThrowException_whenClaimNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoResourceFoundException.class, () -> {
            fileUploadService.uploadFile(99L, "CLAIM_FORM", file);
        });
    }

    @Test
    void uploadFile_shouldThrowException_whenStorageFails() {
        Long claimId = 1L;
        Claim claim = Claim.builder().id(claimId).build();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(storageProvider.storeFile(file)).thenThrow(new RuntimeException("Storage full"));

        assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadFile(claimId, "CLAIM_FORM", file);
        });
    }
}
