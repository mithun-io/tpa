package com.tpa.repository;

import com.tpa.entity.User;
import com.tpa.enums.Gender;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest

@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.tpa.repository.RefreshTokenRepository refreshTokenRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .username("johnDoe")
                .email("john@tpa.com")
                .mobile("1112223333")
                .dateOfBirth(LocalDate.of(1992, 3, 20))
                .address("789 Oak Lane")
                .password("hashed_password")
                .gender(Gender.MALE)
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        savedUser = userRepository.save(user);
    }

    @Test
    @DisplayName("TC-U01: findByEmail returns user when email exists")
    void findByEmail_shouldReturnUser_whenEmailExists() {
        Optional<User> found = userRepository.findByEmail("john@tpa.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("johnDoe");
    }

    @Test
    @DisplayName("TC-U02: findByEmail returns empty for non-existing email")
    void findByEmail_shouldReturnEmpty_whenEmailMissing() {
        Optional<User> found = userRepository.findByEmail("ghost@tpa.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("TC-U03: existsByEmail returns true for existing email")
    void existsByEmail_shouldReturnTrue() {
        assertThat(userRepository.existsByEmail("john@tpa.com")).isTrue();
    }

    @Test
    @DisplayName("TC-U04: existsByEmail returns false for missing email")
    void existsByEmail_shouldReturnFalse() {
        assertThat(userRepository.existsByEmail("missing@tpa.com")).isFalse();
    }

    @Test
    @DisplayName("TC-U05: existsByEmailAndMobile returns true for matching pair")
    void existsByEmailAndMobile_shouldReturnTrue_whenBothMatch() {
        assertThat(userRepository.existsByEmailAndMobile("john@tpa.com", "1112223333")).isTrue();
    }

    @Test
    @DisplayName("TC-U06: existsByEmailAndMobile returns false when mobile mismatch")
    void existsByEmailAndMobile_shouldReturnFalse_whenMobileMismatch() {
        assertThat(userRepository.existsByEmailAndMobile("john@tpa.com", "0000000000")).isFalse();
    }

    @Test
    @DisplayName("TC-U07: findByUsernameContaining returns matching results")
    void findByUsernameContaining_shouldReturnMatches() {
        Page<User> page = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                "john", "john", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("TC-U08: Save persists user with correct role and status")
    void save_shouldPersistRoleAndStatus() {
        Optional<User> found = userRepository.findById(savedUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(found.get().getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
