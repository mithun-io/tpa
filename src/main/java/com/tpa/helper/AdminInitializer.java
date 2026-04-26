package com.tpa.helper;

import com.tpa.entity.User;
import com.tpa.enums.Gender;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.tpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${admin.name}")
    private String adminName;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.mobile}")
    private String adminMobile;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("admin account initialization started...");
            User user = User.builder()
                    .username(adminName)
                    .mobile(adminMobile)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .gender(Gender.MALE)
                    .address("bangalore")
                    .dateOfBirth(LocalDate.parse("2005-06-14"))
                    .userRole(UserRole.FMG_ADMIN)
                    .userStatus(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            log.info("admin account initialized successfully!!");
        } else {
            log.info("admin account exists, initialization aborted.");
        }
    }
}