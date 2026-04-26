package com.tpa.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.TestcontainersConfiguration;
import com.tpa.RequiresDocker;
import com.tpa.dto.request.LoginRequest;
import com.tpa.entity.User;
import com.tpa.enums.Gender;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.tpa.repository.RefreshTokenRepository;
import com.tpa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Tests – verifies JWT auth and role-based access
 * using a real Spring context backed by Testcontainers PostgreSQL + Redis.
 */
@RequiresDocker
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Persist a real ACTIVE customer with BCrypt-hashed password
        userRepository.save(User.builder()
                .username("securityTestUser")
                .email("sec@tpa.com")
                .mobile("5550001111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("1 Security Lane")
                .password(passwordEncoder.encode("Password@123"))
                .gender(Gender.MALE)
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ── TC-SEC-01: Public endpoints ────────────────────────────────────────────

    @Test
    @DisplayName("TC-SEC-01: /auth/login is publicly accessible (no token required)")
    void authLogin_shouldBePublic_andReturn400OrOk() throws Exception {
        // Sending an empty body – endpoint is reachable (no 401/403), returns 400 or similar
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403 : "Auth endpoint should be publicly accessible";
                });
    }

    @Test
    @DisplayName("TC-SEC-02: Swagger UI is publicly accessible")
    void swaggerUi_shouldBePublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403 : "Swagger UI should be public";
                });
    }

    // ── TC-SEC-03: Login with valid credentials returns JWT ────────────────────

    @Test
    @DisplayName("TC-SEC-03: Login with correct credentials returns accessToken in response")
    void login_shouldReturnJwt_whenCredentialsAreCorrect() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("sec@tpa.com");
        req.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    @DisplayName("TC-SEC-04: Login with wrong password returns 401 or 400")
    void login_shouldFail_whenPasswordIsWrong() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("sec@tpa.com");
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 400 || status == 401 || status == 403
                            : "Expected failure status but got: " + status;
                });
    }

    // ── TC-SEC-05: Protected endpoints without token ───────────────────────────

    @Test
    @DisplayName("TC-SEC-05: GET /claims without token returns 401 or 403")
    void getClaims_shouldReturn401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403 : "Expected 401 or 403 but got: " + status;
                });
    }

    @Test
    @DisplayName("TC-SEC-06: GET /admin/users without token returns 401 or 403")
    void getAdminUsers_shouldReturn401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403 : "Expected 401 or 403 but got: " + status;
                });
    }

    // ── TC-SEC-07: Role enforcement via @WithMockUser ─────────────────────────

    @Test
    @WithMockUser(username = "customer@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-SEC-07: CUSTOMER cannot access /admin/users → 403")
    void adminUsersEndpoint_shouldForbid_whenCustomerRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-SEC-08: CUSTOMER cannot block users → 403")
    void blockUser_shouldForbid_whenCustomerRole() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/1/block"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "carrier@ins.com", roles = {"CARRIER_USER"})
    @DisplayName("TC-SEC-09: CARRIER_USER cannot access admin claims → 403")
    void adminClaims_shouldForbid_whenCarrierRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/claims"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-SEC-10: FMG_ADMIN can access admin claims → 200")
    void adminClaims_shouldAllow_whenAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/claims"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@tpa.com", roles = {"FMG_ADMIN"})
    @DisplayName("TC-SEC-11: FMG_ADMIN can list carriers → 200")
    void adminCarriers_shouldAllow_whenAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/carriers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer@tpa.com", roles = {"CUSTOMER"})
    @DisplayName("TC-SEC-12: CUSTOMER can access AI validate-claim endpoint → not 403")
    void aiValidateClaim_shouldNotForbid_whenAuthenticated() throws Exception {
        // Endpoint accepts any authenticated user; bad request body → 400, not 403
        mockMvc.perform(post("/api/v1/ai/validate-claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "Authenticated user should not be forbidden";
                });
    }
}
