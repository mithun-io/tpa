package com.tpa.service;

import com.tpa.enums.ClaimStatus;
import com.tpa.exception.ConflictException;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ClaimStateMachine {

    private final MeterRegistry meterRegistry;

    public ClaimStateMachine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void validateTransition(ClaimStatus currentStatus, ClaimStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new ConflictException("Claim is already in " + currentStatus + " status.");
        }

        boolean isValid = switch (currentStatus) {
            case SUBMITTED -> targetStatus == ClaimStatus.AI_VALIDATED || targetStatus == ClaimStatus.ADMIN_APPROVED || targetStatus == ClaimStatus.REJECTED;
            case AI_VALIDATED -> targetStatus == ClaimStatus.UNDER_REVIEW || targetStatus == ClaimStatus.ADMIN_APPROVED || targetStatus == ClaimStatus.REJECTED;
            case UNDER_REVIEW -> targetStatus == ClaimStatus.ADMIN_APPROVED || targetStatus == ClaimStatus.REJECTED;
            case ADMIN_APPROVED -> targetStatus == ClaimStatus.CARRIER_APPROVED || targetStatus == ClaimStatus.REJECTED;
            case CARRIER_APPROVED -> targetStatus == ClaimStatus.PAYMENT_PENDING || targetStatus == ClaimStatus.SETTLED;
            case PAYMENT_PENDING -> targetStatus == ClaimStatus.SETTLED;
            case REJECTED, SETTLED -> false; // Terminal states
            default -> false;
        };

        if (!isValid) {
            meterRegistry.counter("claims.transition.invalid", "from", currentStatus.name(), "to", targetStatus.name()).increment();
            throw new ConflictException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        if (targetStatus == ClaimStatus.CARRIER_APPROVED) {
            meterRegistry.counter("claims.approved.total").increment();
        } else if (targetStatus == ClaimStatus.REJECTED) {
            meterRegistry.counter("claims.rejected.total").increment();
        }
    }
}
