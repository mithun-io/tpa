package com.tpa.service.impl;

import com.tpa.dto.request.CustomerRequest;
import com.tpa.dto.request.LoginRequest;
import com.tpa.dto.request.PasswordResetRequest;
import com.tpa.dto.request.OtpRequest;
import com.tpa.dto.response.LoginResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.entity.Customer;
import com.tpa.entity.RefreshToken;
import com.tpa.entity.User;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.tpa.exception.BadRequestException;
import com.tpa.exception.ConflictException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.helper.CustomUserDetails;
import com.tpa.helper.CustomUserDetailsService;
import com.tpa.helper.EmailService;
import com.tpa.helper.RedisService;
import com.tpa.mapper.CustomerMapper;
import com.tpa.mapper.UserMapper;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.CustomerRepository;
import com.tpa.repository.PatientRepository;
import com.tpa.repository.UserRepository;
import com.tpa.security.JwtUtil;
import com.tpa.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserMapper userMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private RedisService redisService;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private SecureRandom secureRandom;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private CustomerRequest validCustomerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setEmail("test@tpa.com");
        validCustomerRequest.setMobile("1234567890");
        validCustomerRequest.setName("Test User");
        validCustomerRequest.setPassword("Pass@123");

        testUser = User.builder()
                .id(1L)
                .email("test@tpa.com")
                .mobile("1234567890")
                .password("encoded_pass")
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC-001: Register customer with valid data")
    void customerRegistration_shouldStorePendingAndSendOtp_whenValid() {
        when(userRepository.existsByEmailAndMobile("test@tpa.com", "1234567890")).thenReturn(false);
        when(redisService.isPendingCustomerExists("test@tpa.com")).thenReturn(false);
        when(secureRandom.nextInt(100000, 1000000)).thenReturn(123456);

        authService.customerRegistration(validCustomerRequest);

        verify(emailService).sendOtp("Test User", "test@tpa.com", 123456);
        verify(redisService).storeOtp("test@tpa.com", 123456);
        verify(redisService).storePendingCustomer("test@tpa.com", validCustomerRequest);
    }

    @Test
    @DisplayName("TC-002: Register with existing email throws exception")
    void customerRegistration_shouldThrowConflict_whenUserExists() {
        when(userRepository.existsByEmailAndMobile(any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.customerRegistration(validCustomerRequest));
        verify(emailService, never()).sendOtp(any(), any(), anyInt());
    }

    @Test
    @DisplayName("TC-003: OTP verification success activates user")
    void verifyOtp_shouldCreateUser_whenOtpIsValid() {
        when(redisService.getOtp("test@tpa.com")).thenReturn(123456);
        when(redisService.getPendingCustomer("test@tpa.com")).thenReturn(validCustomerRequest);
        when(passwordEncoder.encode("Pass@123")).thenReturn("encoded_pass");

        when(userRepository.save(any())).thenReturn(testUser);

        OtpRequest request = new OtpRequest();
        request.setEmail("test@tpa.com");
        request.setOtp("123456");
        authService.verifyCustomerOtp(request);

        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
        verify(redisService).deletePendingCustomer("test@tpa.com");
    }

    @Test
    @DisplayName("TC-004: OTP invalid throws exception")
    void verifyOtp_shouldThrowBadRequest_whenOtpIsInvalid() {
        when(redisService.getOtp("test@tpa.com")).thenReturn(111111);
        when(redisService.getPendingCustomer("test@tpa.com")).thenReturn(validCustomerRequest);

        OtpRequest request = new OtpRequest();
        request.setEmail("test@tpa.com");
        request.setOtp("000000");
        assertThrows(BadRequestException.class, () -> authService.verifyCustomerOtp(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-006: Login success returns JWT")
    void login_shouldReturnJwt_whenCredentialsValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@tpa.com");
        request.setPassword("Pass@123");
        when(userRepository.findByEmail("test@tpa.com")).thenReturn(Optional.of(testUser));
        
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        when(customUserDetailsService.loadUserByUsername("test@tpa.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mock-jwt");

        RefreshToken rToken = new RefreshToken();
        rToken.setToken("mock-refresh");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(rToken);
        when(userMapper.toUserResponse(testUser)).thenReturn(new UserResponse());

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("TC-007: Login invalid password throws error")
    void login_shouldThrowException_whenBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@tpa.com");
        request.setPassword("wrong");
        when(userRepository.findByEmail("test@tpa.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("TC-008: Login inactive user throws error")
    void login_shouldThrowBadRequest_whenUserInactive() {
        testUser.setUserStatus(UserStatus.BLOCKED);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@tpa.com");
        request.setPassword("Pass@123");
        when(userRepository.findByEmail("test@tpa.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("TC-011: Forgot password OTP sent successfully")
    void forgotPassword_shouldSendOtp_whenEmailExists() {
        when(userRepository.findByEmail("test@tpa.com")).thenReturn(Optional.of(testUser));
        when(secureRandom.nextInt(anyInt(), anyInt())).thenReturn(999999);

        authService.forgetPassword("test@tpa.com");

        verify(emailService).sendOtp(isNull(), eq("test@tpa.com"), eq(999999));
        verify(redisService).storeOtp("test@tpa.com", 999999);
    }

    @Test
    @DisplayName("TC-012: Reset password success")
    void resetPassword_shouldUpdatePassword_whenOtpValid() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@tpa.com");
        request.setOtp("123456");
        request.setNewPassword("NewPass@123");
        
        when(userRepository.findByEmail("test@tpa.com")).thenReturn(Optional.of(testUser));
        when(redisService.getOtp("test@tpa.com")).thenReturn(123456);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new_encoded");

        authService.passwordReset(request);

        verify(userRepository).save(argThat(u -> u.getPassword().equals("new_encoded")));
    }
}
