package com.tpa.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentOrderRequest(
        @NotNull Long claimId,
        @NotNull @Positive Double amount
) {}
