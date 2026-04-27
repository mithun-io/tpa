package com.tpa.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private PdfExportService pdfExportService;

    @Test
    void exportClaimReport_shouldReturnPdfBytes_whenClaimExists() throws IOException {
        User user = User.builder()
                .username("John Doe")
                .email("john@doe.com")
                .build();

        Claim claim = Claim.builder()
                .id(1L)
                .policyNumber("POL-12345")
                .status(ClaimStatus.CARRIER_APPROVED)
                .amount(10000.0)
                .user(user)
                .createdDate(LocalDateTime.now())
                .patientName("John Doe")
                .hospitalName("City Hospital")
                .build();

        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        byte[] pdfBytes = pdfExportService.exportClaimReport(1L);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        // Verify PDF Content
        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String textFromPage = extractor.getTextFromPage(1);

        assertThat(textFromPage).contains("TPA CLAIM DECISION REPORT");
        assertThat(textFromPage).contains("POL-12345");
        assertThat(textFromPage).contains("John Doe");
        assertThat(textFromPage).contains("City Hospital");
        assertThat(textFromPage).contains("CARRIER_APPROVED");
    }

    @Test
    void exportClaimReport_shouldThrowException_whenClaimNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoResourceFoundException.class, () -> {
            pdfExportService.exportClaimReport(99L);
        });
    }
}
