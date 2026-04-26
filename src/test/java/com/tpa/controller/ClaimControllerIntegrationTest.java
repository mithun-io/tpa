package com.tpa.controller;

import com.tpa.TestcontainersConfiguration;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ClaimControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ClaimService claimService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @org.junit.jupiter.api.Test
    @WithMockUser(username = "admin@tpa.com", roles = {"FMG_ADMIN"})
    public void searchClaims_shouldReturn200_whenAdminIsAuthenticated() throws Exception {
        ClaimResponse claim1 = new ClaimResponse();
        claim1.setId(1L);
        claim1.setStatus(ClaimStatus.APPROVED);

        Page<ClaimResponse> page = new PageImpl<>(List.of(claim1));
        when(claimService.searchClaims(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/claims/search?status=APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    @org.junit.jupiter.api.Test
    public void searchClaims_shouldReturn401_whenNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/claims/search?status=APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403 : "Expected 401 or 403 but got: " + status;
                });
    }
}
