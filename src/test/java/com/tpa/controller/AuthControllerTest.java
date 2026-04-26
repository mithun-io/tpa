package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.request.LoginRequest;
import com.tpa.dto.request.OtpRequest;
import com.tpa.dto.request.RefreshTokenRequest;
import com.tpa.dto.response.LoginResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== Login ==========

    @Test
    void login_shouldReturn200WithToken_whenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@tpa.com");
        request.setPassword("Password@123");  // must satisfy @Pattern on LoginRequest

        LoginResponse loginResponse = new LoginResponse("jwt-token", "refresh-token", new UserResponse());

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("login successful"));
    }

    @Test
    void login_shouldReturn404_whenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@tpa.com");
        request.setPassword("Password@123");

        when(authService.login(any())).thenThrow(new NoResourceFoundException("user not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_shouldReturn401_whenBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@tpa.com");
        request.setPassword("Password@123");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ========== Customer Registration ==========

    @Test
    void customerRegistration_shouldReturn201_whenValidRequest() throws Exception {
        String body = """
                {
                  "name": "John Doe",
                  "email": "john@tpa.com",
                  "mobile": "9876543210",
                  "password": "Secure@123",
                  "address": "123 Main St",
                  "gender": "MALE",
                  "dateOfBirth": "1990-01-01"
                }
                """;
        doNothing().when(authService).customerRegistration(any());

        mockMvc.perform(post("/api/v1/auth/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("otp sent successfully"));
    }

    // ========== OTP Verify ==========

    @Test
    void verifyCustomerOtp_shouldReturn200_whenOtpIsValid() throws Exception {
        OtpRequest request = new OtpRequest();
        request.setEmail("john@tpa.com");
        request.setOtp("123456");  // must be exactly 6 digits per @Pattern

        doNothing().when(authService).verifyCustomerOtp(any());

        mockMvc.perform(patch("/api/v1/auth/customer/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("registration successful"));
    }

    // ========== Refresh Token ==========

    @Test
    void refreshToken_shouldReturn200_whenTokenIsValid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        LoginResponse loginResponse = new LoginResponse("new-jwt-token", "valid-refresh-token", new UserResponse());
        when(authService.refreshToken(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.message").value("token refreshed"));
    }
}
