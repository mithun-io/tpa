package com.tpa.controller;

import com.tpa.service.ClaimService;
import com.tpa.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tpa.TestcontainersConfiguration;
import com.tpa.RequiresDocker;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RequiresDocker
class SecurityAndUploadTest {

    @Autowired 
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should reject unauthorized access to claim timeline")
    void getTimeline_Unauthenticated_Returns401Or403() throws Exception {
        mockMvc.perform(get("/api/v1/claims/1/timeline"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403;
                });
    }

    @Test
    @WithMockUser(username = "customer@tpa.com", roles = "CUSTOMER")
    @DisplayName("Should allow customer to access their own timeline")
    void getTimeline_Authorized_Returns200() throws Exception {
        com.tpa.dto.response.ClaimResponse mockClaim = com.tpa.dto.response.ClaimResponse.builder()
                .id(1L)
                .userEmail("customer@tpa.com")
                .build();
        org.mockito.Mockito.when(claimService.getClaim(org.mockito.ArgumentMatchers.anyLong())).thenReturn(mockClaim);
        org.mockito.Mockito.when(claimService.getClaimAudits(org.mockito.ArgumentMatchers.anyLong())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/claims/1/timeline"))
                .andExpect(status().isOk());
    }
}
