package com.tpa.service.impl;

import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.entity.Claim;
import com.tpa.repository.ClaimRepository;
import com.tpa.repository.ClaimDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AiValidationEdgeCaseTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimDocumentRepository claimDocumentRepository;

    @InjectMocks
    private AiClaimAssistantServiceImpl aiService;

    @Test
    @DisplayName("AI should return fallback summary if API call fails")
    void generateSummary_ApiFailure_ReturnsFallback() {
        Claim claim = Claim.builder().id(1L).patientName("John Doe").build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        
        // Mocking the builder chain for RestClient
        lenient().when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenThrow(new RuntimeException("API Down"));

        String response = aiService.generateClaimSummary(1L);
        
        assertNotNull(response);
        assertTrue(response.contains("Unable to generate AI summary"));
    }

    @Test
    @DisplayName("AI should handle claim analysis failure gracefully")
    void analyzeClaim_ApiFailure_ReturnsFallbackResponse() {
        Claim claim = Claim.builder().id(1L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimDocumentRepository.findByClaimId(1L)).thenReturn(java.util.List.of());
        
        lenient().when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenThrow(new RuntimeException("API Down"));

        AiAnalysisResponse response = aiService.analyzeClaim(1L, "Analyze this");
        
        assertNotNull(response);
        assertEquals("Manual review required due to AI failure", response.getRecommendation());
    }
}
