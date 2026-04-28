package com.tpa.service.impl;

import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.mapper.ClaimMapper;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private CarrierRepository carrierRepository;

    @Mock
    private ClaimMapper claimMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    @DisplayName("Verify admin assignment of carrier to a claim")
    void assignCarrier_shouldAssignCarrier_whenClaimAndCarrierExist() {
        Claim claim = new Claim();
        claim.setId(100L);

        com.tpa.entity.User user = new com.tpa.entity.User();
        user.setUserStatus(com.tpa.enums.UserStatus.ACTIVE);

        Carrier carrier = new Carrier();
        carrier.setId(10L);
        carrier.setUser(user);

        when(claimRepository.findById(100L)).thenReturn(Optional.of(claim));
        when(carrierRepository.findById(10L)).thenReturn(Optional.of(carrier));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);
        
        ClaimResponse claimResponse = new ClaimResponse();
        claimResponse.setId(100L);
        when(claimMapper.toDto(claim)).thenReturn(claimResponse);

        ClaimResponse result = adminService.assignCarrierToClaim(100L, 10L);

        assertThat(claim.getCarrier()).isEqualTo(carrier);
        assertThat(result.getId()).isEqualTo(100L);
        verify(claimRepository).save(claim);
    }
}
