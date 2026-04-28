package com.tpa.service.impl;

import com.tpa.dto.request.*;
import com.tpa.dto.response.LoginResponse;
import com.tpa.dto.response.UserResponse;
import com.tpa.entity.Customer;
import com.tpa.entity.Patient;
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
import com.tpa.repository.CustomerRepository;
import com.tpa.repository.PatientRepository;
import com.tpa.repository.UserRepository;
import com.tpa.security.JwtUtil;
import com.tpa.service.AuthService;
import com.tpa.service.RefreshTokenService;
import com.tpa.entity.RefreshToken;
import com.tpa.repository.RefreshTokenRepository;
import com.tpa.entity.Carrier;
import com.tpa.repository.CarrierRepository;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.service.impl.CarrierAiValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PatientRepository patientRepository;
    private final CarrierRepository carrierRepository;

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;

    private final CustomUserDetailsService customUserDetailsService;
    private final RedisService redisService;
    private final EmailService emailService;

    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ClaimEventProducer claimEventProducer;
    private final CarrierAiValidationService carrierAiValidationService;

    private Integer generateOtp() {
        return secureRandom.nextInt(100000, 1000000);
    }

    @Transactional
    @Override
    public void customerRegistration(CustomerRequest customerRequest) {
        if (userRepository.existsByEmailAndMobile(customerRequest.getEmail(), customerRequest.getMobile())) {
            throw new ConflictException("user already exists");
        }
        if (redisService.isPendingCustomerExists(customerRequest.getEmail())) {
            throw new ConflictException("pending registration already exists");
        }

        int otp = generateOtp();
        emailService.sendOtp(customerRequest.getName(), customerRequest.getEmail(), otp);
        redisService.storeOtp(customerRequest.getEmail(), otp);
        redisService.storePendingCustomer(customerRequest.getEmail(), customerRequest);
    }

    @Transactional
    @Override
    public void patientRegistration(PatientRequest patientRequest) {
        if (userRepository.existsByEmailAndMobile(patientRequest.getEmail(), patientRequest.getPhoneNumber())) {
            throw new ConflictException("user already exists");
        }
        if (redisService.isPendingPatientExists(patientRequest.getEmail())) {
            throw new ConflictException("pending registration already exists");
        }

        int otp = generateOtp();
        emailService.sendOtp(patientRequest.getPatientName(), patientRequest.getEmail(), otp);
        redisService.storeOtp(patientRequest.getEmail(), otp);
        redisService.storePendingPatient(patientRequest.getEmail(), patientRequest);
    }

    @Transactional
    @Override
    public void carrierRegistration(CarrierRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("user already exists");
        }
        if (carrierRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new ConflictException("a carrier with this registration number already exists");
        }
        if (redisService.isPendingCarrierExists(request.getEmail())) {
            throw new ConflictException("pending carrier registration already exists");
        }

        int otp = generateOtp();
        emailService.sendOtp(request.getCompanyName(), request.getEmail(), otp);
        redisService.storeOtp(request.getEmail(), otp);
        redisService.storePendingCarrier(request.getEmail(), request);
    }

    @Transactional
    @Override
    public void verifyCarrierOtp(OtpRequest otpRequest) {
        Integer storedOtp = redisService.getOtp(otpRequest.getEmail());
        CarrierRegistrationRequest stored = redisService.getPendingCarrier(otpRequest.getEmail());

        if (storedOtp == null) {
            throw new BadRequestException("otp expired or invalid");
        }
        if (stored == null) {
            throw new BadRequestException("no pending carrier registration found");
        }
        if (!String.valueOf(storedOtp).equals(otpRequest.getOtp())) {
            throw new BadRequestException("invalid otp");
        }
        if (userRepository.existsByEmail(stored.getEmail())) {
            throw new ConflictException("user already exists");
        }
        if (carrierRepository.existsByRegistrationNumber(stored.getRegistrationNumber())) {
            throw new ConflictException("a carrier with registration number '" + stored.getRegistrationNumber() + "' already exists");
        }

        User user = User.builder()
                .username(stored.getCompanyName())
                .email(stored.getEmail())
                .mobile(stored.getMobile())
                .dateOfBirth(java.time.LocalDate.now())
                .address(stored.getAddress())
                .password(passwordEncoder.encode(stored.getPassword()))
                .gender(null)
                .userRole(UserRole.CARRIER_USER)
                .userStatus(UserStatus.PENDING)   // PENDING until admin approves
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Carrier carrier = Carrier.builder()
                .user(user)
                .companyName(stored.getCompanyName())
                .registrationNumber(stored.getRegistrationNumber())
                .companyType(stored.getCompanyType())
                .licenseNumber(stored.getLicenseNumber())
                .taxId(stored.getTaxId())
                .contactPersonName(stored.getContactPersonName())
                .contactPersonPhone(stored.getContactPersonPhone())
                .website(stored.getWebsite())
                .build();
        carrier = carrierRepository.save(carrier);

        emailService.sendConfirmation(user.getUsername(), user.getEmail(), stored.getPassword());
        redisService.deleteOtp(otpRequest.getEmail());
        redisService.deletePendingCarrier(otpRequest.getEmail());

        // ── Fire AI validation + Kafka AFTER the DB commit (non-blocking) ──────
        // This is what makes OTP verify fast (<200ms). Heavy work runs post-commit.
        final Long carrierId = carrier.getId();
        final String companyName = carrier.getCompanyName();
        final String email = user.getEmail();
        final Carrier carrierRef = carrier;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("[POST-COMMIT] Running AI validation + Kafka for carrier {}", carrierId);
                try {
                    carrierAiValidationService.validateAndScore(carrierRef);
                } catch (Exception ex) {
                    log.error("Post-commit AI validation failed for carrier {}: {}", carrierId, ex.getMessage());
                }
                try {
                    claimEventProducer.publishCarrierCreatedEvent(carrierId, companyName, email);
                } catch (Exception ex) {
                    log.warn("Post-commit Kafka publish failed for carrier {}: {}", carrierId, ex.getMessage());
                }
            }
        });

        log.info("verifyCarrierOtp completed synchronously. AI+Kafka deferred to post-commit.");
    }

    @Transactional
    @Override
    public void verifyCustomerOtp(OtpRequest otpRequest) {
        Integer storedOtp = redisService.getOtp(otpRequest.getEmail());
        CustomerRequest storedCustomer = redisService.getPendingCustomer(otpRequest.getEmail());

        if (storedOtp == null) {
            throw new BadRequestException("otp expired or invalid");
        }
        if (storedCustomer == null) {
            throw new BadRequestException("no pending registration found");
        }
        if (!String.valueOf(storedOtp).equals(otpRequest.getOtp())) {
            throw new BadRequestException("invalid otp");
        }
        if (userRepository.existsByEmail(storedCustomer.getEmail())) {
            throw new ConflictException("user already exists!");
        }

        User user = User.builder()
                .username(storedCustomer.getName())
                .email(storedCustomer.getEmail())
                .mobile(storedCustomer.getMobile())
                .dateOfBirth(storedCustomer.getDateOfBirth())
                .address(storedCustomer.getAddress())
                .password(passwordEncoder.encode(storedCustomer.getPassword()))
                .gender(storedCustomer.getGender())
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .build();
        customerRepository.save(customer);

        emailService.sendConfirmation(user.getUsername(), user.getEmail(), user.getPassword());
        redisService.deleteOtp(otpRequest.getEmail());
        redisService.deletePendingCustomer(otpRequest.getEmail());
    }

    @Transactional
    @Override
    public void verifyPatientOtp(OtpRequest otpRequest) {
        Integer storedOtp = redisService.getOtp(otpRequest.getEmail());
        PatientRequest storedPatient = redisService.getPendingPatient(otpRequest.getEmail());

        if (storedOtp == null) {
            throw new BadRequestException("otp expired or invalid");
        }
        if (storedPatient == null) {
            throw new BadRequestException("no pending registration found");
        }
        if (!String.valueOf(storedOtp).equals(otpRequest.getOtp())) {
            throw new BadRequestException("invalid otp");
        }
        if (userRepository.existsByEmail(storedPatient.getEmail())) {
            throw new ConflictException("user already exists!");
        }

        User user = User.builder()
                .username(storedPatient.getPatientName())
                .email(storedPatient.getEmail())
                .mobile(storedPatient.getPhoneNumber())
                .dateOfBirth(storedPatient.getDateOfBirth())
                .address(storedPatient.getAddress())
                .password(passwordEncoder.encode(storedPatient.getPassword()))
                .gender(storedPatient.getGender())
                .userRole(UserRole.CUSTOMER) // Assign CUSTOMER role to Patients
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Patient patient = Patient.builder()
                .user(user)
                .build();
        patientRepository.save(patient);

        emailService.sendConfirmation(user.getUsername(), user.getEmail(), user.getPassword());
        redisService.deleteOtp(otpRequest.getEmail());
        redisService.deletePendingPatient(otpRequest.getEmail());
    }

    @Transactional
    @Override
    public void resendOtp(String email) {

        CustomerRequest customer = redisService.getPendingCustomer(email);
        PatientRequest patient = redisService.getPendingPatient(email);

        if (customer == null && patient == null) {
            throw new BadRequestException("no pending registration found");
        }

        redisService.deleteOtp(email);

        int otp = generateOtp();

        if (customer != null) {
            emailService.sendOtp(customer.getName(), customer.getEmail(), otp);
            redisService.storeOtp(customer.getEmail(), otp);
        } else {
            emailService.sendOtp(patient.getPatientName(), patient.getEmail(), otp);
            redisService.storeOtp(patient.getEmail(), otp);
        }
    }

    @Transactional
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() -> new NoResourceFoundException("user not found"));

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            if (user.getUserRole() == UserRole.CARRIER_USER && user.getUserStatus() == UserStatus.INACTIVE) {
                throw new BadRequestException("Your carrier account is pending admin approval. You will be notified once approved.");
            }
            if (user.getUserRole() == UserRole.CARRIER_USER && user.getUserStatus() == UserStatus.BLOCKED) {
                throw new BadRequestException("Your carrier account has been rejected. Please contact support.");
            }
            throw new BadRequestException("account is not active. current status: " + user.getUserStatus().name().toLowerCase());
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        CustomUserDetails customUserDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(loginRequest.getEmail());
        String token = jwtUtil.generateToken(customUserDetails);
        log.info("{} logged in successfully", user.getUsername());

        UserResponse userResponse = userMapper.toUserResponse(user);
        
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        return new LoginResponse(token, refreshToken.getToken(), userResponse);
    }

    @Transactional
    @Override
    public UserResponse passwordChange(PasswordChangeRequest passwordChangeRequest, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NoResourceFoundException("user not found"));

        if (!passwordEncoder.matches(passwordChangeRequest.getPreviousPassword(), user.getPassword())) {
            throw new BadRequestException("password is incorrect");
        }

        if (passwordEncoder.matches(passwordChangeRequest.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("new password cannot be same as previous password");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    @Override
    public void forgetPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NoResourceFoundException("user not found"));

        int otp = generateOtp();
        emailService.sendOtp(user.getUsername(), email, otp);
        redisService.storeOtp(email, otp);
    }

    @Transactional
    @Override
    public void passwordReset(PasswordResetRequest passwordResetRequest) {
        User user = userRepository.findByEmail(passwordResetRequest.getEmail()).orElseThrow(() -> new NoResourceFoundException("user not found"));

        Integer storedOtp = redisService.getOtp(passwordResetRequest.getEmail());
        if (storedOtp == null) {
            throw new BadRequestException("otp expired or invalid");
        }
        if (!String.valueOf(storedOtp).equals(passwordResetRequest.getOtp())) {
            throw new BadRequestException("invalid otp");
        }
        if (passwordEncoder.matches(passwordResetRequest.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("new password cannot be same as previous password");
        }

        user.setPassword(passwordEncoder.encode(passwordResetRequest.getNewPassword()));
        userRepository.save(user);

        emailService.sendConfirmation(user.getUsername(), user.getEmail(), user.getPassword());
        redisService.deleteOtp(passwordResetRequest.getEmail());
    }

    @Transactional
    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    CustomUserDetails customUserDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(user.getEmail());
                    String token = jwtUtil.generateToken(customUserDetails);
                    UserResponse userResponse = userMapper.toUserResponse(user);
                    return new LoginResponse(token, request.getRefreshToken(), userResponse);
                })
                .orElseThrow(() -> new BadRequestException("Refresh token is not in database!"));
    }
}