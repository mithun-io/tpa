package com.tpa.controller;

import com.tpa.entity.ClaimDocument;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== Upload ==========

    @Test
    void uploadFile_shouldReturn200_whenValidFileIsUploaded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        ClaimDocument document = ClaimDocument.builder().id(10L).fileName("test.pdf").build();
        when(fileUploadService.uploadFile(eq(1L), eq("CLAIM_FORM"), any(MultipartFile.class)))
                .thenReturn(document);

        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("claimId", "1")
                .param("documentType", "CLAIM_FORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.document.id").value(10))
                .andExpect(jsonPath("$.document.fileName").value("test.pdf"));
    }

    @Test
    void uploadFile_shouldReturn400_whenNoFileProvided() throws Exception {
        // No file part → Spring throws MissingServletRequestParameterException → 400
        mockMvc.perform(multipart("/api/v1/files/upload")
                .param("claimId", "1")
                .param("documentType", "CLAIM_FORM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_shouldReturn404_whenClaimNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy".getBytes());

        when(fileUploadService.uploadFile(eq(99L), any(), any(MultipartFile.class)))
                .thenThrow(new NoResourceFoundException("Claim not found"));

        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("claimId", "99")
                .param("documentType", "CLAIM_FORM"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadFile_shouldReturn500_whenStorageFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy".getBytes());

        when(fileUploadService.uploadFile(eq(1L), any(), any(MultipartFile.class)))
                .thenThrow(new RuntimeException("Disk full"));

        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("claimId", "1")
                .param("documentType", "CLAIM_FORM"))
                .andExpect(status().isInternalServerError());
    }

    // ========== Download ==========

    @Test
    void downloadFile_shouldReturnResourceWithCorrectHeaders_whenFileExists() throws Exception {
        Resource resource = new ByteArrayResource("pdf content".getBytes());
        ClaimDocument document = ClaimDocument.builder().id(1L).fileName("report.pdf").build();

        when(fileUploadService.downloadFile(1L)).thenReturn(resource);
        when(fileUploadService.getDocument(1L)).thenReturn(document);

        mockMvc.perform(get("/api/v1/files/download/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"report.pdf\""))
                .andExpect(content().bytes("pdf content".getBytes()));
    }

    @Test
    void downloadFile_shouldReturn404_whenDocumentNotFound() throws Exception {
        when(fileUploadService.downloadFile(99L))
                .thenThrow(new NoResourceFoundException("Document not found"));

        mockMvc.perform(get("/api/v1/files/download/99"))
                .andExpect(status().isNotFound());
    }
}
