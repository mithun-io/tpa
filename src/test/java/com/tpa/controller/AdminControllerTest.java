package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.request.ClaimReviewRequest;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.dto.response.CustomerResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== 200 OK ==========

    @Test
    void blockUser_shouldReturn200_whenAdminBlocksUser() throws Exception {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);

        when(adminService.blockUser(1L)).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/admin/users/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void unblockUser_shouldReturn200_whenAdminUnblocksUser() throws Exception {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(2L);

        when(adminService.unblockUser(2L)).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/admin/users/2/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getAllCustomers_shouldReturn200_whenAdminFetchesCustomers() throws Exception {
        CustomerResponse customer = new CustomerResponse();
        when(adminService.getAllCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/v1/admin/customers"))
                .andExpect(status().isOk());
    }

    @Test
    void reviewClaim_shouldReturn200_whenAdminReviewsClaim() throws Exception {
        ClaimReviewRequest request = new ClaimReviewRequest();
        request.setClaimId(1L);
        request.setStatus(ClaimStatus.ADMIN_APPROVED);
        request.setReviewNotes("Approved after review");

        ClaimResponse claimResponse = new ClaimResponse();
        claimResponse.setId(1L);
        claimResponse.setStatus(ClaimStatus.ADMIN_APPROVED);

        when(adminService.reviewClaim(any(), any())).thenReturn(claimResponse);

        mockMvc.perform(post("/api/v1/admin/claims/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_APPROVED"));
    }

    @Test
    void getClaimAiSummary_shouldReturn200_whenAdminRequestsSummary() throws Exception {
        AiAnalysisResponse analysisResponse = new AiAnalysisResponse();
        analysisResponse.setRecommendation("This claim looks valid.");
        when(adminService.getClaimAiSummary(1L)).thenReturn(analysisResponse);

        mockMvc.perform(get("/api/v1/admin/claims/1/ai-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("This claim looks valid."));
    }

    // ========== Service-level Error Propagation ==========

    @Test
    void blockUser_shouldReturn404_whenUserNotFound() throws Exception {
        when(adminService.blockUser(99L)).thenThrow(new NoResourceFoundException("user not found"));

        mockMvc.perform(patch("/api/v1/admin/users/99/block"))
                .andExpect(status().isNotFound());
    }

    // NOTE: 401/403 role-based tests (security filter chain) are covered in
    // ClaimProcessingIntegrationTest using @SpringBootTest + @WithMockUser.
}
