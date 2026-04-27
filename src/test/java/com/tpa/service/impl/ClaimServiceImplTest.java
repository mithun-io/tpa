package com.tpa.service.impl;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.entity.Customer;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.BadRequestException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.mapper.ClaimMapper;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.repository.UserRepository;
import com.tpa.service.AuditLogService;
import com.tpa.service.ClaimStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private UserRepository userRepository;
    @Mock private CarrierRepository carrierRepository;
    @Mock private ClaimMapper claimMapper;
    @Mock private ClaimEventProducer claimEventProducer;
    @Mock private com.tpa.kafka.producer.ProducerService producerService;
    @Mock private AuditLogService auditLogService;
    @Mock private ClaimStateMachine claimStateMachine;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private User testUser;
    private Claim testClaim;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@tpa.com").build();
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setUser(testUser);
        
        testClaim = Claim.builder()
                .id(100L)
                .status(ClaimStatus.PENDING)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("TC-016: Create claim valid")
    void createClaim_shouldReturnClaimResponse_whenUserExists() {
        ClaimDataRequest request = new ClaimDataRequest();
        request.setClaimedAmount(1000.0);
        request.setPolicyNumber("POL-123");

        when(userRepository.findByEmail("user@tpa.com")).thenReturn(Optional.of(testUser));
        when(claimRepository.save(any(Claim.class))).thenReturn(testClaim);
        
        ClaimResponse mockResponse = new ClaimResponse();
        mockResponse.setId(100L);
        when(claimMapper.toDto(testClaim)).thenReturn(mockResponse);

        ClaimResponse response = claimService.createClaim(request, "user@tpa.com");

        assertThat(response.getId()).isEqualTo(100L);
        verify(claimRepository).save(any(Claim.class));
        verify(auditLogService).logAction(eq(100L), eq("CLAIM_CREATED"), isNull(), eq(ClaimStatus.PENDING));
    }

    @Test
    @DisplayName("TC-017: Create claim invalid user")
    void createClaim_shouldThrowException_whenUserNotFound() {
        ClaimDataRequest request = new ClaimDataRequest();
        when(userRepository.findByEmail("unknown@tpa.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> claimService.createClaim(request, "unknown@tpa.com"));
    }

    @Test
    @DisplayName("TC-018: Fetch claim by ID exists")
    void getClaim_shouldReturnResponse_whenExists() {
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));
        when(claimMapper.toDto(testClaim)).thenReturn(new ClaimResponse());

        ClaimResponse response = claimService.getClaim(100L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("TC-019: Fetch invalid claim")
    void getClaim_shouldThrow404_whenNotExists() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> claimService.getClaim(999L));
    }

    @Test
    @DisplayName("TC-021: Status transitions")
    void processClaimDecision_shouldUpdateStatus_whenDecisionIsApproved() {
        ClaimDecisionResponse decision = new ClaimDecisionResponse();
        decision.setStatus(ClaimStatus.APPROVED);

        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));
        doNothing().when(claimStateMachine).validateTransition(any(), any());
        
        claimService.processClaimDecision(100L, decision);

        assertThat(testClaim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        verify(claimRepository, times(2)).save(testClaim); 
    }

    @Test
    @DisplayName("TC-022: Prevent invalid transition")
    void processClaimDecision_shouldSkip_whenAlreadyInTerminalState() {
        testClaim.setStatus(ClaimStatus.APPROVED);
        ClaimDecisionResponse decision = new ClaimDecisionResponse();

        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        claimService.processClaimDecision(100L, decision);

        verify(claimRepository, never()).save(any());
        verify(claimStateMachine, never()).validateTransition(any(), any());
    }

    @Test
    @DisplayName("TC-023: Pagination & TC-024: Search by status")
    void searchClaims_shouldReturnPaginated_whenRequested() {
        Page<Claim> claimPage = new PageImpl<>(List.of(testClaim));
        when(claimRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(claimPage);
        when(claimMapper.toDto(any())).thenReturn(new ClaimResponse());

        Page<ClaimResponse> response = claimService.searchClaims(ClaimStatus.REVIEW, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
    }

}
