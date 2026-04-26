package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.service.AiClaimAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiClaimAssistantService aiClaimAssistantService;

    @InjectMocks
    private AiController aiController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== /analyze/{claimId} ==========

    @Test
    void analyzeClaim_shouldReturn200WithResponse_whenValidClaimAndPrompt() throws Exception {
        AiAnalysisResponse analysisResponse = new AiAnalysisResponse();
        analysisResponse.setRecommendation("Claim looks valid with no anomalies.");
        when(aiClaimAssistantService.analyzeClaim(anyLong(), anyString()))
                .thenReturn(analysisResponse);

        mockMvc.perform(post("/api/v1/ai/analyze/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("prompt", "Summarize this claim"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("Claim looks valid with no anomalies."));
    }

    @Test
    void analyzeClaim_shouldUseDefaultPrompt_whenNoBodyProvided() throws Exception {
        AiAnalysisResponse analysisResponse = new AiAnalysisResponse();
        analysisResponse.setRecommendation("Default AI summary.");
        when(aiClaimAssistantService.analyzeClaim(anyLong(), anyString()))
                .thenReturn(analysisResponse);

        // No request body → controller uses the hardcoded default prompt
        mockMvc.perform(post("/api/v1/ai/analyze/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("Default AI summary."));
    }

    @Test
    void analyzeClaim_shouldReturnFallbackMessage_whenAiServiceFails() throws Exception {
        AiAnalysisResponse analysisResponse = new AiAnalysisResponse();
        analysisResponse.setRecommendation("AI Analysis failed. Error: Connection Timeout");
        when(aiClaimAssistantService.analyzeClaim(anyLong(), anyString()))
                .thenReturn(analysisResponse);

        mockMvc.perform(post("/api/v1/ai/analyze/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("prompt", "Summarize"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("AI Analysis failed. Error: Connection Timeout"));
    }

}
