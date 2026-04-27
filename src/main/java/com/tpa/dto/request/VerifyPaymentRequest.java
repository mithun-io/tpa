package com.tpa.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(
        @NotBlank String razorpay_order_id,
        @NotBlank String razorpay_payment_id,
        @NotBlank String razorpay_signature
) {}
