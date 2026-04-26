package com.tpa.service;

import com.tpa.dto.request.*;
import com.tpa.dto.response.LoginResponse;
import com.tpa.dto.response.UserResponse;

import java.security.Principal;

public interface AuthService {

    void customerRegistration(CustomerRequest customerRequest);

    void patientRegistration(PatientRequest patientRequest);

    void carrierRegistration(CarrierRegistrationRequest request);

    void verifyCarrierOtp(OtpRequest request);

    void verifyCustomerOtp(OtpRequest otpRequest);

    void verifyPatientOtp(OtpRequest otpRequest);

    void resendOtp(String email);

    LoginResponse login(LoginRequest loginRequest);

    UserResponse passwordChange(PasswordChangeRequest passwordChangeRequest, Principal principal);

    void forgetPassword(String email);

    void passwordReset(PasswordResetRequest passwordResetRequest);

    LoginResponse refreshToken(RefreshTokenRequest request);
}