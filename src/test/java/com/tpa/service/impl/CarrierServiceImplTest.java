package com.tpa.service.impl;

import com.tpa.dto.response.CarrierClaimDetailResponse;
import com.tpa.dto.response.PolicyStatusResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.enums.ClaimStatus;
import com.tpa.exception.BadRequestException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.producer.ProducerService;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierServiceImplTest {

    @Mock private CarrierRepository carrierRepository;
    @Mock private ClaimRepository claimRepository;
    @Mock private ProducerService producerService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CarrierServiceImpl carrierService;

    private Carrier testCarrier;
    private Claim testClaim;
    private User customer;

    @BeforeEach
    void setUp() {
        User carrierUser = new User();
        carrierUser.setEmail("carrier@tpa.com");
        
        testCarrier = new Carrier();
        testCarrier.setId(10L);
        testCarrier.setCompanyName("Test Carrier");
        testCarrier.setUser(carrierUser);

        customer = new User();
        customer.setEmail("customer@test.com");

        testClaim = new Claim();
        testClaim.setId(100L);
        testClaim.setCarrier(testCarrier);
        testClaim.setUser(customer);
        testClaim.setStatus(ClaimStatus.PENDING);
        testClaim.setRiskScore(50.0);
        testClaim.setPolicyNumber("POL-1234");
        testClaim.setAmount(1000.0);
    }

    @Test
    @DisplayName("TC-CARRIER-01: Get Assigned Claims successfully")
    void getAssignedClaims_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findByCarrier_Id(10L)).thenReturn(List.of(testClaim));

        List<CarrierClaimDetailResponse> res = carrierService.getAssignedClaims("carrier@tpa.com");
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getClaimId()).isEqualTo(100L);
        assertThat(res.get(0).getPolicy().getStatus()).isEqualTo("VALID");
    }

    @Test
    @DisplayName("TC-CARRIER-02: Get single claim detail successfully")
    void getClaimDetail_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        CarrierClaimDetailResponse res = carrierService.getClaimDetail(100L, "carrier@tpa.com");
        assertThat(res.getClaimId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("TC-CARRIER-03: Validate Policy successfully")
    void validatePolicy_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        carrierService.validatePolicy(100L, "carrier@tpa.com");
        verify(claimRepository).save(testClaim);
        assertThat(testClaim.getReviewNotes()).contains("Policy validated by Test Carrier");
    }

    @Test
    @DisplayName("TC-CARRIER-04: Approve claim notifies admin")
    void approveClaim_success_and_notifies_admin() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        carrierService.approveClaim(100L, "carrier@tpa.com");

        assertThat(testClaim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        verify(claimRepository).save(testClaim);
        verify(producerService).sendClaimNotificationEvent(any());
        verify(notificationService).notifyAllAdmins(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-CARRIER-05: Reject claim successfully")
    void rejectClaim_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        carrierService.rejectClaim(100L, "carrier@tpa.com");

        assertThat(testClaim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        verify(claimRepository).save(testClaim);
        verify(producerService).sendClaimNotificationEvent(any());
    }

    @Test
    @DisplayName("TC-CARRIER-06: Add Remark successfully")
    void addRemark_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        carrierService.addRemark(100L, "Looks good", "carrier@tpa.com");
        assertThat(testClaim.getReviewNotes()).contains("Looks good");
    }

    @Test
    @DisplayName("TC-CARRIER-07: Flag suspicious caps risk score")
    void flagSuspicious_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));
        
        testClaim.setRiskScore(90.0); // should cap at 100

        carrierService.flagSuspicious(100L, "carrier@tpa.com");

        assertThat(testClaim.getRiskScore()).isEqualTo(100.0);
        assertThat(testClaim.getRiskFlags()).contains("SUSPICIOUS");
    }

    @Test
    @DisplayName("TC-CARRIER-08: Get Policy Status works")
    void getPolicyStatus_success() {
        when(carrierRepository.findByUser_Email("carrier@tpa.com")).thenReturn(Optional.of(testCarrier));
        when(claimRepository.findById(100L)).thenReturn(Optional.of(testClaim));

        PolicyStatusResponse res = carrierService.getPolicyStatus(100L, "carrier@tpa.com");
        assertThat(res.getStatus()).isEqualTo("VALID");
    }
}
