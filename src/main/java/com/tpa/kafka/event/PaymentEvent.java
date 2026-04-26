package com.tpa.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {

    private String eventType;

    private Long orderId;

    private Long userId;

    private String email;

    private Double amount;

    private String paymentStatus;

    private String paymentMethod;

    private String transactionId;

    private String timestamp;
}