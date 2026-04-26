package com.tpa.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.request.ClaimDataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishClaimCreatedEvent(Long claimId, ClaimDataRequest request) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("claimId", claimId);
            event.put("data", request);
            
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("claim-created", String.valueOf(claimId), message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize claim event", e);
        }
    }

    public void publishCarrierCreatedEvent(Long carrierId, String companyName, String email) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("carrierId", carrierId);
            event.put("companyName", companyName);
            event.put("email", email);

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("carrier-created", String.valueOf(carrierId), message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize carrier event", e);
        }
    }

    public void publishCarrierApprovedEvent(Long carrierId, String companyName, String email) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("carrierId", carrierId);
            event.put("companyName", companyName);
            event.put("email", email);
            event.put("action", "APPROVED");

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("carrier-approved", String.valueOf(carrierId), message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize carrier-approved event", e);
        }
    }
}
