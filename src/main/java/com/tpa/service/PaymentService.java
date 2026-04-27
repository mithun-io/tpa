package com.tpa.service;

import com.tpa.dto.request.CreatePaymentOrderRequest;
import com.tpa.dto.request.VerifyPaymentRequest;
import com.tpa.dto.response.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    Map<String, Object> createOrder(Long userId, CreatePaymentOrderRequest request);
    PaymentResponse verifyPayment(VerifyPaymentRequest request);
    PaymentResponse getPaymentByClaimId(Long claimId);
}
