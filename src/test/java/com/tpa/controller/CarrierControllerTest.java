package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.dto.response.CarrierClaimDetailResponse;
import com.tpa.enums.Verdict;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.service.AiClaimAssistantService;
import com.tpa.service.CarrierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CarrierControllerTest {

    @Mock private CarrierService carrierService;
    @Mock private AiClaimAssistantService aiClaimAssistantService;

    @InjectMocks
    private CarrierController carrierController;

    private MockMvc mockMvc;
    private Principal mockPrincipal;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(carrierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("carrier@tpa.com", null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("TC-CARRIER-09: Get Assigned Claims via API")
    void getAssignedClaims_success() throws Exception {
        CarrierClaimDetailResponse res = CarrierClaimDetailResponse.builder().claimId(1L).build();
        when(carrierService.getAssignedClaims("carrier@tpa.com")).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/carrier/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].claimId").value(1));
    }

    @Test
    @DisplayName("TC-CARRIER-10: Get Claim Detail via API")
    void getClaimDetail_success() throws Exception {
        CarrierClaimDetailResponse res = CarrierClaimDetailResponse.builder().claimId(1L).build();
        when(carrierService.getClaimDetail(1L, "carrier@tpa.com")).thenReturn(res);

        mockMvc.perform(get("/api/v1/carrier/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimId").value(1));
    }

    @Test
    @DisplayName("TC-CARRIER-11: Approve claim via API")
    void approveClaim_success() throws Exception {
        doNothing().when(carrierService).approveClaim(1L, "carrier@tpa.com");

        mockMvc.perform(patch("/api/v1/carrier/claims/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Claim approved successfully"));
    }

    @Test
    @DisplayName("TC-CARRIER-12: Trigger AI Analysis via API")
    void aiAnalyze_success() throws Exception {
        AiAnalysisResponse aiRes = new AiAnalysisResponse();
        aiRes.setVerdict(Verdict.APPROVED);
        aiRes.setRiskScore(10.0);
        
        when(aiClaimAssistantService.analyzeClaim(eq(1L), anyString())).thenReturn(aiRes);

        mockMvc.perform(post("/api/v1/carrier/claims/1/ai-analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("prompt", "analyze"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verdict").value("APPROVED"));
    }

    @Test
    @DisplayName("TC-CARRIER-13: Unauthorized role test")
    void unauthorized_access_fails() throws Exception {
        // Clear auth to simulate unauthenticated access
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        
        // This will result in 400 or 500 error since the context is null in currentUser()
        mockMvc.perform(get("/api/v1/carrier/claims"))
                .andExpect(status().is5xxServerError()); 
    }
}
