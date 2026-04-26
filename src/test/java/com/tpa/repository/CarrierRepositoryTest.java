package com.tpa.repository;

import com.tpa.TestcontainersConfiguration;
import com.tpa.RequiresDocker;
import com.tpa.entity.Carrier;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDocker
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CarrierRepositoryTest {

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.tpa.repository.RefreshTokenRepository refreshTokenRepository;

    private Carrier savedCarrier;
    private User carrierUser;

    @BeforeEach
    void setUp() {
        carrierRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        carrierUser = User.builder()
                .username("carrieruser")
                .email("carrier@ins.com")
                .mobile("9876543210")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .address("456 Business Ave")
                .password("secret")
                .gender(Gender.MALE)
                .userRole(UserRole.CARRIER_USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        carrierUser = userRepository.save(carrierUser);

        savedCarrier = Carrier.builder()
                .user(carrierUser)
                .companyName("SecureLife Insurance")
                .registrationNumber("REG-001")
                .companyType("Private")
                .licenseNumber("LIC-001")
                .taxId("TAX-001")
                .contactPersonName("Alice")
                .contactPersonPhone("9999999999")
                .website("https://securelife.com")
                .aiRiskScore(0.2)
                .aiRiskStatus("LOW_RISK")
                .aiRecommendation("SAFE_TO_APPROVE")
                .build();
        savedCarrier = carrierRepository.save(savedCarrier);
    }

    @Test
    @DisplayName("TC-R01: findByUser_Email returns carrier for valid email")
    void findByUserEmail_shouldReturnCarrier_whenEmailExists() {
        Optional<Carrier> result = carrierRepository.findByUser_Email("carrier@ins.com");

        assertThat(result).isPresent();
        assertThat(result.get().getCompanyName()).isEqualTo("SecureLife Insurance");
    }

    @Test
    @DisplayName("TC-R02: findByUser_Email returns empty for non-existing email")
    void findByUserEmail_shouldReturnEmpty_whenEmailNotFound() {
        Optional<Carrier> result = carrierRepository.findByUser_Email("unknown@ins.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-R03: findByUser_Username returns carrier for valid username")
    void findByUsername_shouldReturnCarrier_whenUsernameExists() {
        Optional<Carrier> result = carrierRepository.findByUser_Username("carrieruser");

        assertThat(result).isPresent();
        assertThat(result.get().getRegistrationNumber()).isEqualTo("REG-001");
    }

    @Test
    @DisplayName("TC-R04: findByUser_Id returns carrier for valid user ID")
    void findByUserId_shouldReturnCarrier_whenUserIdExists() {
        Optional<Carrier> result = carrierRepository.findByUser_Id(carrierUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getLicenseNumber()).isEqualTo("LIC-001");
    }

    @Test
    @DisplayName("TC-R05: findByCompanyNameIgnoreCase returns carrier regardless of case")
    void findByCompanyName_shouldBeCaseInsensitive() {
        Optional<Carrier> result = carrierRepository.findByCompanyNameIgnoreCase("SECURELIFE INSURANCE");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedCarrier.getId());
    }

    @Test
    @DisplayName("TC-R06: Save carrier persists AI risk fields")
    void save_shouldPersistAiRiskFields() {
        Optional<Carrier> found = carrierRepository.findById(savedCarrier.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAiRiskScore()).isEqualTo(0.2);
        assertThat(found.get().getAiRiskStatus()).isEqualTo("LOW_RISK");
        assertThat(found.get().getAiRecommendation()).isEqualTo("SAFE_TO_APPROVE");
    }

    @Test
    @DisplayName("TC-R07: Delete carrier removes it from DB")
    void delete_shouldRemoveCarrier() {
        Long carrierId = savedCarrier.getId();
        carrierRepository.deleteById(carrierId);
        assertThat(carrierRepository.findById(carrierId)).isEmpty();
    }
}
