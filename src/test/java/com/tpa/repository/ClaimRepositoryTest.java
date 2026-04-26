package com.tpa.repository;

import com.tpa.TestcontainersConfiguration;
import com.tpa.RequiresDocker;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDocker
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.tpa.repository.ClaimDocumentRepository claimDocumentRepository;

    @Autowired
    private com.tpa.repository.RefreshTokenRepository refreshTokenRepository;

    private User savedUser;
    private Claim claim1;
    private Claim claim2;

    @BeforeEach
    void setUp() {
        claimDocumentRepository.deleteAll();
        claimRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .mobile("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("123 Main St")
                .password("password")
                .gender(Gender.MALE)
                .userRole(UserRole.CUSTOMER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        savedUser = userRepository.save(user);

        claim1 = Claim.builder()
                .policyNumber("POL-001")
                .status(ClaimStatus.PENDING)
                .amount(1500.0)
                .user(savedUser)
                .build();
        claimRepository.save(claim1);

        claim2 = Claim.builder()
                .policyNumber("POL-002")
                .status(ClaimStatus.APPROVED)
                .amount(2500.0)
                .user(savedUser)
                .build();
        claimRepository.save(claim2);
    }

    // ── TC-36: Basic CRUD ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-036: findByUserId returns claims for existing user")
    void findByUserId_shouldReturnClaims_whenUserIdExists() {
        List<Claim> claims = claimRepository.findByUserId(savedUser.getId());

        assertThat(claims).hasSize(2);
        assertThat(claims).extracting(Claim::getPolicyNumber)
                .containsExactlyInAnyOrder("POL-001", "POL-002");
    }

    @Test
    @DisplayName("TC-037: findByUserId returns empty list for non-existing user")
    void findByUserId_shouldReturnEmpty_whenUserDoesNotExist() {
        List<Claim> claims = claimRepository.findByUserId(99999L);
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("TC-038: existsByPolicyNumber returns true for existing policy")
    void existsByPolicyNumber_shouldReturnTrue_whenPolicyExists() {
        boolean exists = claimRepository.existsByPolicyNumber("POL-001");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("TC-039: existsByPolicyNumber returns false for missing policy")
    void existsByPolicyNumber_shouldReturnFalse_whenPolicyDoesNotExist() {
        boolean exists = claimRepository.existsByPolicyNumber("POL-999");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("TC-040: findById returns claim when it exists")
    void findById_shouldReturnClaim_whenExists() {
        Optional<Claim> found = claimRepository.findById(claim1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPolicyNumber()).isEqualTo("POL-001");
    }

    // ── TC-41: Specification / Search ─────────────────────────────────────────

    @Test
    @DisplayName("TC-041: Specification search by PENDING status returns correct count")
    void searchClaims_shouldReturnPagedResults_whenFilteringByStatus() {
        Specification<Claim> spec = Specification.where(ClaimSpecification.hasStatus(ClaimStatus.PENDING));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Claim> page = claimRepository.findAll(spec, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPolicyNumber()).isEqualTo("POL-001");
    }

    @Test
    @DisplayName("TC-042: Specification search by amount range returns correct claim")
    void searchClaims_shouldReturnClaims_whenAmountBetween() {
        Specification<Claim> spec = ClaimSpecification.amountBetween(2000.0, 3000.0);
        List<Claim> claims = claimRepository.findAll(spec);

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getPolicyNumber()).isEqualTo("POL-002");
    }

    @Test
    @DisplayName("TC-043: Specification search by username returns matching claims")
    void searchClaims_shouldReturnClaims_whenUserMatches() {
        Specification<Claim> spec = ClaimSpecification.hasUser("testuser");
        List<Claim> claims = claimRepository.findAll(spec);

        assertThat(claims).hasSize(2);
    }

    @Test
    @DisplayName("TC-044: Specification null status predicate returns all claims")
    void searchClaims_withNullStatus_shouldReturnAll() {
        Specification<Claim> spec = Specification.where(ClaimSpecification.hasStatus(null));
        List<Claim> claims = claimRepository.findAll(spec);

        assertThat(claims).hasSize(2);
    }

    // ── TC-45: Sorting ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-045: Sorting by createdDate desc returns claims in correct order")
    void findAll_shouldSortByCreatedDateDesc_whenRequested() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Claim> page = claimRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(2);
        if (page.getContent().get(0).getCreatedDate() != null
                && page.getContent().get(1).getCreatedDate() != null) {
            assertThat(page.getContent().get(0).getCreatedDate())
                    .isAfterOrEqualTo(page.getContent().get(1).getCreatedDate());
        }
    }

    // ── TC-46: Custom JPQL Queries ────────────────────────────────────────────

    @Test
    @DisplayName("TC-046: countClaimsByStatus returns grouped results")
    void countClaimsByStatus_shouldGroupCorrectly() {
        List<Object[]> results = claimRepository.countClaimsByStatus();

        assertThat(results).isNotEmpty();
        boolean hasPending = results.stream()
                .anyMatch(r -> r[0].toString().equals("PENDING") && ((Number) r[1]).longValue() == 1);
        assertThat(hasPending).isTrue();
    }

    @Test
    @DisplayName("TC-047: sumApprovedClaimAmount returns correct sum")
    void sumApprovedClaimAmount_shouldReturnTotal() {
        Double total = claimRepository.sumApprovedClaimAmount();
        assertThat(total).isNotNull().isEqualTo(2500.0);
    }

    @Test
    @DisplayName("TC-048: countClaimsPerDay returns data for a date range")
    void countClaimsPerDay_shouldReturnDailyBreakdown() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        List<Object[]> results = claimRepository.countClaimsPerDay(startDate);

        assertThat(results).isNotEmpty();
    }

    // ── TC-49: Persistence validation ─────────────────────────────────────────

    @Test
    @DisplayName("TC-049: Saved claim persists all mandatory fields correctly")
    void save_shouldPersistAllMandatoryFields() {
        Claim claim = Claim.builder()
                .policyNumber("POL-FULL")
                .status(ClaimStatus.PENDING)
                .amount(3000.0)
                .patientName("John Doe")
                .hospitalName("City Hospital")
                .diagnosis("Fracture")
                .user(savedUser)
                .build();
        Claim saved = claimRepository.save(claim);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPatientName()).isEqualTo("John Doe");
        assertThat(saved.getHospitalName()).isEqualTo("City Hospital");
        assertThat(saved.getDiagnosis()).isEqualTo("Fracture");
        assertThat(saved.getCreatedDate()).isNotNull(); // auto set by @PrePersist
    }

    @Test
    @DisplayName("TC-050: Claim deletion removes from DB")
    void delete_shouldRemoveClaim_fromDatabase() {
        Long id = claim1.getId();
        claimRepository.deleteById(id);
        assertThat(claimRepository.findById(id)).isEmpty();
    }
}
