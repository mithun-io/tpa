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
            case PENDING -> targetStatus == ClaimStatus.PROCESSING || targetStatus == ClaimStatus.REJECTED;
            case PROCESSING -> targetStatus == ClaimStatus.PENDING || targetStatus == ClaimStatus.REVIEW || targetStatus == ClaimStatus.REJECTED;
            case REVIEW -> targetStatus == ClaimStatus.APPROVED || targetStatus == ClaimStatus.REJECTED;
            case APPROVED -> targetStatus == ClaimStatus.PAYMENT_PENDING;
            case PAYMENT_PENDING -> targetStatus == ClaimStatus.SETTLED || targetStatus == ClaimStatus.FAILED;
            case REJECTED, SETTLED, FAILED -> false; // Terminal states
            default -> false;
        };

        if (!isValid) {
            meterRegistry.counter("claims.transition.invalid", "from", currentStatus.name(), "to", targetStatus.name()).increment();
            throw new ConflictException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        if (targetStatus == ClaimStatus.APPROVED) {
            meterRegistry.counter("claims.approved.total").increment();
        } else if (targetStatus == ClaimStatus.REJECTED) {
            meterRegistry.counter("claims.rejected.total").increment();
        }
    }
}
