package com.tpa.controller;

import com.tpa.dto.request.*;
import com.tpa.dto.response.ApiResponse;
import com.tpa.dto.response.LoginResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<Void>> customerRegistration(@Valid @RequestBody CustomerRequest customerRequest) {
        authService.customerRegistration(customerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "otp sent successfully", null, 201));
    }

    @PatchMapping("/customer/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCustomerOtp(@Valid @RequestBody OtpRequest otpRequest) {
        authService.verifyCustomerOtp(otpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "registration successful", null, 200));
    }

    @PostMapping("/patient/register")
    public ResponseEntity<ApiResponse<Void>> patientRegistration(@Valid @RequestBody PatientRequest patientRequest) {
        authService.patientRegistration(patientRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "otp sent successfully", null, 201));
    }

    @PatchMapping("/patient/verify")
    public ResponseEntity<ApiResponse<Void>> verifyPatientOtp(@Valid @RequestBody OtpRequest otpRequest) {
        authService.verifyPatientOtp(otpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "registration successful", null, 200));
    }

    @PostMapping("/carrier/register")
    public ResponseEntity<ApiResponse<Void>> carrierRegistration(@Valid @RequestBody CarrierRegistrationRequest request) {
        authService.carrierRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "OTP sent to carrier email. Please verify.", null, 201));
    }

    @PatchMapping("/carrier/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCarrierOtp(@Valid @RequestBody OtpRequest otpRequest) {
        authService.verifyCarrierOtp(otpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Carrier registered successfully", null, 200));
    }

    @PatchMapping("/resend-otp/{email}")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@PathVariable("email") String email) {
        authService.resendOtp(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "otp resent successfully", null, 200));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(new ApiResponse<>(true, "login successful", authService.login(loginRequest), 200));
    }

    @PatchMapping("/password-change")
    public ResponseEntity<ApiResponse<UserResponse>> passwordChange(@Valid @RequestBody PasswordChangeRequest passwordChangeRequest, Principal principal) {
        return ResponseEntity.ok().body(new ApiResponse<>(true, "password changed successfully", authService.passwordChange(passwordChangeRequest, principal), 200));
    }

    @PatchMapping("/forget-password/{email}")
    public ResponseEntity<ApiResponse<Void>> forgetPassword(@PathVariable("email") String email) {
        authService.forgetPassword(email);
        return ResponseEntity.ok().body(new ApiResponse<>(true, "otp sent successfully, verify to reset password", null, 200));
    }

    @PatchMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> passwordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        authService.passwordReset(passwordResetRequest);
        return ResponseEntity.ok().body(new ApiResponse<>(true, "password reset successful", null, 200));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "token refreshed", authService.refreshToken(request), 200));
    }
}