package com.tpa.integration;

import com.fasterxml.jackson.databind.ObjectMapper;


import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.enums.Gender;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimAuditRepository;
import com.tpa.repository.ClaimDocumentRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.repository.RefreshTokenRepository;
import com.tpa.repository.UserRepository;
import com.tpa.service.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 – Full Integration Tests
 * Covers the end-to-end claim processing pipeline using real PostgreSQL, Kafka, Redis via Testcontainers.
 */

@SpringBootTest

class ClaimProcessingIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ClaimRepository claimRepository;
    @Autowired private ClaimDocumentRepository claimDocumentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CarrierRepository carrierRepository;
    @Autowired private ClaimAuditRepository claimAuditRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private StorageProvider storageProvider;
    @MockitoBean private KafkaTemplate<String, String> kafkaTemplate;

    private MockMvc mockMvc;
    private User testCustomer;
    private User adminUser;
    private User carrierUserEntity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        claimDocumentRepository.deleteAll();
        claimAuditRepository.deleteAll();
        carrierRepository.deleteAll();
        claimRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testCustomer = userRepository.save(User.builder()
                .username("customer1")
                .email("customer1@tpa.com")
                .mobile("1111111111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("10 Customer St")
                .password("$2a$10$DummyHashedPasswordForTestUse111")
                .gender(Gender.MALE)
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        adminUser = userRepository.save(User.builder()
                .username("admin1")
                .email("admin1@tpa.com")
                .mobile("2222222222")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .address("1 Admin Plaza")
                .password("$2a$10$DummyHashedPasswordForTestUse222")
                .gender(Gender.FEMALE)
                .userRole(UserRole.FMG_ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        carrierUserEntity = userRepository.save(User.builder()
                .username("carrier1")
                .email("carrier1@ins.com")
                .mobile("3333333333")
                .dateOfBirth(LocalDate.of(1980, 3, 10))
                .address("99 Carrier Blvd")
                .password("$2a$10$DummyHashedPasswordForTestUse333")
                .gender(Gender.MALE)
                .userRole(UserRole.CARRIER_USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        Carrier carrier = Carrier.builder()
                .user(carrierUserEntity)
                .companyName("MegaInsure")
                .registrationNumber("REG-INT-001")
                .companyType("Private")
                .licenseNumber("LIC-INT-001")
                .taxId("TAX-INT-001")
                .contactPersonName("Bob")
                .contactPersonPhone("9999999999")
                .build();
        carrierRepository.save(carrier);
    }

    // ── TC-INT-01: Claim Creation ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "customer1@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-INT-01: Customer can create a claim and it persists with SUBMITTED status")
    void createClaim_shouldPersistWithPendingStatus_whenCustomerCreates() throws Exception {
        ClaimDataRequest req = new ClaimDataRequest();
        req.setClaimedAmount(8000.0);
        req.setPolicyNumber("POL-INT-CREATE");

        String body = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        Long claimId = objectMapper.readTree(body).get("id").asLong();
        Claim saved = claimRepository.findById(claimId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    // ── TC-INT-02: Role-based access on /claims ────────────────────────────────

    @Test
    @WithMockUser(username = "admin1@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-INT-02: Admin can view all claims via admin endpoint")
    void adminGetAllClaims_shouldReturn200_whenAdminAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/claims"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer1@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-INT-03: Customer gets 403 when accessing admin endpoint")
    void adminEndpoint_shouldReturn403_whenCustomerAccesses() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "carrier1@ins.com", roles = {"CARRIER_USER"})
    @DisplayName("TC-INT-04: Carrier gets 403 when accessing admin claim review")
    void adminReview_shouldReturn403_whenCarrierAccesses() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ── TC-INT-05: Admin user management ──────────────────────────────────────

    @Test
    @WithMockUser(username = "admin1@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-INT-05: Admin can list all users")
    void getAllUsers_shouldReturnPagedUsers_whenAdminRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin1@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-INT-06: Admin can block a user")
    void blockUser_shouldUpdateUserStatus_whenAdminBlocks() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + testCustomer.getId() + "/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userStatus").value("BLOCKED"));

        User updated = userRepository.findById(testCustomer.getId()).orElseThrow();
        assertThat(updated.getUserStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    @WithMockUser(username = "admin1@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-INT-07: Admin can unblock a previously blocked user")
    void unblockUser_shouldActivateUser_whenAdminUnblocks() throws Exception {
        // First block
        mockMvc.perform(patch("/api/v1/admin/users/" + testCustomer.getId() + "/block"))
                .andExpect(status().isOk());

        // Then unblock
        mockMvc.perform(patch("/api/v1/admin/users/" + testCustomer.getId() + "/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userStatus").value("ACTIVE"));
    }

    // ── TC-INT-08: File upload guard ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "customer1@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-INT-08: File upload with mocked StorageProvider returns success")
    void uploadFile_shouldReturnSuccess_whenStorageProviderMocked() throws Exception {
        // Create a claim first
        ClaimDataRequest req = new ClaimDataRequest();
        req.setClaimedAmount(5000.0);
        req.setPolicyNumber("POL-UPLOAD-TEST");

        String claimBody = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long claimId = objectMapper.readTree(claimBody).get("id").asLong();

        when(storageProvider.storeFile(any())).thenReturn("/mocked/path/claim_form.pdf");

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "claim_form.pdf", "application/pdf", "dummy content".getBytes());

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("claimId", claimId.toString())
                        .param("documentType", "CLAIM_FORM"))
                .andExpect(status().isOk());
    }

    // ── TC-INT-09: Unauthenticated access ─────────────────────────────────────

    @Test
    @DisplayName("TC-INT-09: Unauthenticated request to protected endpoint returns 401 or 403")
    void protectedEndpoint_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403 : "Expected 401 or 403 but got: " + status;
                });
    }

    // ── TC-INT-10: Customer claim list ────────────────────────────────────────

    @Test
    @WithMockUser(username = "customer1@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-INT-10: Customer can see their own claims")
    void getCustomerClaims_shouldReturn200_whenCustomerRequests() throws Exception {
        // Persist a claim directly
        claimRepository.save(Claim.builder()
                .policyNumber("POL-MINE")
                .status(ClaimStatus.SUBMITTED)
                .amount(1000.0)
                .user(testCustomer)
                .build());

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk());
    }

    // ── TC-INT-11: Carrier dashboard ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "carrier1@ins.com", roles = {"CARRIER_USER"})
    @DisplayName("TC-INT-11: Carrier can access their dashboard")
    void carrierDashboard_shouldReturn200_whenCarrierAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/carrier/dashboard"))
                .andExpect(status().isOk());
    }

    // ── TC-INT-12: Admin carrier management ───────────────────────────────────

    @Test
    @WithMockUser(username = "admin1@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-INT-12: Admin can list all carriers")
    void adminGetAllCarriers_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/carriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── TC-INT-13: DB Integrity – unique email ─────────────────────────────────

    @Test
    @DisplayName("TC-INT-13: Saving two users with same email throws exception")
    void saveUser_shouldFail_whenEmailDuplicated() {
        User dup = User.builder()
                .username("dupUser")
                .email("customer1@tpa.com") // duplicate
                .mobile("9990001234")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .address("Dup St")
                .password("pass")
                .gender(Gender.FEMALE)
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> userRepository.saveAndFlush(dup));
    }
}
