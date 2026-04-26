package com.tpa.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentEvent(PaymentEvent paymentEvent) {
        try {
            String message = objectMapper.writeValueAsString(paymentEvent);
            kafkaTemplate.send("payment-success", message).whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("kafka sent: {}", message);
                } else {
                    log.error("kafka send failed: {}", message, exception);
                }
            });
        } catch (Exception e) {
            log.error("failed to serialize payment event: {}", paymentEvent, e);
        }
    }

    public void sendClaimNotificationEvent(com.tpa.kafka.event.ClaimNotificationEvent claimNotificationEvent) {
        try {
            String message = objectMapper.writeValueAsString(claimNotificationEvent);
            kafkaTemplate.send("claim-notifications", message).whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("kafka claim notification sent: {}", message);
                } else {
                    log.error("kafka claim notification send failed: {}", message, exception);
                }
            });
        } catch (Exception e) {
            log.error("failed to serialize claim notification event: {}", claimNotificationEvent, e);
        }
    }
}