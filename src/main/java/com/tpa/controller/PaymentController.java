package com.tpa.controller;

import com.tpa.dto.request.CreatePaymentOrderRequest;
import com.tpa.dto.request.VerifyPaymentRequest;
import com.tpa.dto.response.PaymentResponse;
import com.tpa.entity.User;
import com.tpa.repository.UserRepository;
import com.tpa.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    /**
     * Step 1: Admin/Customer initiates payment after claim is APPROVED.
     * Returns Razorpay order details to the frontend for checkout.
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'FMG_ADMIN')")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePaymentOrderRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> orderDetails = paymentService.createOrder(user.getId(), request);
        return ResponseEntity.ok(orderDetails);
    }

    /**
     * Step 2: Frontend calls this after Razorpay checkout completes.
     * Backend verifies signature, marks claim SETTLED.
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'FMG_ADMIN')")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch payment status for a given claim.
     */
    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'FMG_ADMIN', 'FMG_CARRIER')")
    public ResponseEntity<PaymentResponse> getPaymentForClaim(@PathVariable Long claimId) {
        PaymentResponse response = paymentService.getPaymentByClaimId(claimId);
        return ResponseEntity.ok(response);
    }
}
