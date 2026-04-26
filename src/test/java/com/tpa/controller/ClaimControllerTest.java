package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.service.ClaimService;
import com.tpa.service.PdfExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService claimService;

    @Mock
    private PdfExportService pdfExportService;

    @InjectMocks
    private ClaimController claimController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        // PageableHandlerMethodArgumentResolver is needed to resolve Pageable params
        mockMvc = MockMvcBuilders.standaloneSetup(claimController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Set up a mock authentication in SecurityContext so getAuthentication().getName() works
        var auth = new UsernamePasswordAuthenticationToken(
                "testuser@tpa.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createClaim_shouldReturn200_whenValidRequest() throws Exception {
        ClaimDataRequest request = new ClaimDataRequest();
        request.setClaimedAmount(1000.0);

        ClaimResponse response = new ClaimResponse();
        response.setId(1L);
        response.setStatus(ClaimStatus.PENDING);

        when(claimService.createClaim(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getClaim_shouldReturn200_whenClaimExists() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(1L);
        response.setStatus(ClaimStatus.APPROVED);

        when(claimService.getClaim(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void exportClaimReport_shouldReturnPdfWithCorrectFilename_whenClaimExists() throws Exception {
        byte[] pdfContent = "dummy pdf bytes".getBytes();
        when(pdfExportService.exportClaimReport(1L)).thenReturn(pdfContent);

        mockMvc.perform(get("/api/v1/claims/1/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"claim-report-1.pdf\""))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void searchClaims_shouldReturnPagedResults_whenStatusFilterApplied() throws Exception {
        ClaimResponse claim = new ClaimResponse();
        claim.setId(1L);
        claim.setStatus(ClaimStatus.PENDING);
        Page<ClaimResponse> page = new PageImpl<>(List.of(claim), org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(claimService.searchClaims(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/claims/search")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllClaims_shouldReturn200WithList_whenClaimsExist() throws Exception {
        ClaimResponse claim = new ClaimResponse();
        claim.setId(1L);

        when(claimService.getAllClaims()).thenReturn(List.of(claim));

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
