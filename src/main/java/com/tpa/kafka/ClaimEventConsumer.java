package com.tpa.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;
import com.tpa.helper.EmailService;
import com.tpa.service.ClaimService;
import com.tpa.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEventConsumer {

    private final ObjectMapper objectMapper;
    private final RuleEngineService ruleEngineService;
    private final ClaimService claimService;
    private final EmailService emailService;

    @KafkaListener(topics = "claim-created", groupId = "tpa-group")
    public void consumeClaimCreatedEvent(String message) {
        log.info("[KAFKA] Received claim-created event: {}", message);
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long claimId = Long.valueOf(event.get("claimId").toString());
            
            // Idempotency check: Skip if claim is already finalized
            com.tpa.dto.response.ClaimResponse claimResponse = claimService.getClaim(claimId);
            if (claimResponse.getStatus() == com.tpa.enums.ClaimStatus.APPROVED || 
                claimResponse.getStatus() == com.tpa.enums.ClaimStatus.REJECTED) {
                log.info("[KAFKA] Claim {} already finalized with {}. Skipping.", claimId, claimResponse.getStatus());
                return;
            }
            
            // Deserialize the data payload into ClaimDataRequest
            String dataJson = objectMapper.writeValueAsString(event.get("data"));
            ClaimDataRequest request = objectMapper.readValue(dataJson, ClaimDataRequest.class);

            log.info("[KAFKA] Running rule engine for claim {}", claimId);
            // Run Rule Engine
            ClaimDecisionResponse decision = ruleEngineService.evaluateClaim(request);
            log.info("[KAFKA] Rule engine decision for claim {}: status={}, reasons={}", claimId, decision.getStatus(), decision.getReasons());

            claimService.processClaimDecision(claimId, decision);
            log.info("[KAFKA] Claim {} status updated to {}", claimId, decision.getStatus());
        } catch (Exception e) {
            // Do NOT rethrow — prevents infinite retry loops. Log and move on.
            log.error("[KAFKA] Error processing claim event. Message: {}. Error: {}", message, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "claim-notifications", groupId = "tpa-group")
    public void consumeClaimNotificationEvent(String message) {
        try {
            log.info("Received claim-notification event: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<>() {});
            
            Long claimId = Long.valueOf(event.get("claimId").toString());
            String status = event.get("status").toString();
            String reviewNotes = event.get("message").toString();
            
            // Note: In real app, we fetch user email using claimId. 
            // Using a dummy email for mock notification purposes.
            String mockEmail = "customer-" + claimId + "@tpa.com";
            
            emailService.sendClaimStatusNotification(mockEmail, claimId, status, reviewNotes);
            log.info("Sent email notification for claim {}", claimId);
            
        } catch (Exception e) {
            log.error("Error processing claim notification: {}", e.getMessage());
        }
    }
}
