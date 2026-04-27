package com.tpa.dto.response;

import com.tpa.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long claimId,
        Double amount,
        String currency,
        PaymentStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime createdAt
) {}
