package com.tpa.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.entity.Carrier;
import com.tpa.repository.CarrierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierAiValidationService {

    private final CarrierRepository carrierRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String model;

    private static final double AUTO_APPROVE_THRESHOLD = 30.0;

    /**
     * Called after OTP verification.
     * Validates the carrier company using AI and stores results.
     * Auto-approves if riskScore < threshold; otherwise leaves INACTIVE for manual review.
     */
    @Transactional
    public void validateAndScore(Carrier carrier) {
        try {
            String systemPrompt =
                "You are a financial compliance AI. Analyze the following insurance carrier registration details. " +
                "Assess legitimacy, fraud risk, and whether this company should be approved. " +
                "Return ONLY a valid JSON object — no markdown, no explanations.\n" +
                "Schema:\n" +
                "{\n" +
                "  \"riskScore\": 25,\n" +
                "  \"riskStatus\": \"LOW_RISK\",\n" +
                "  \"recommendation\": \"SAFE_TO_APPROVE\",\n" +
                "  \"reason\": \"Company name and email domain appear legitimate.\"\n" +
                "}\n" +
                "riskScore: 0-100 (higher = riskier)\n" +
                "riskStatus: LOW_RISK | MEDIUM_RISK | HIGH_RISK\n" +
                "recommendation: SAFE_TO_APPROVE | MANUAL_REVIEW | REJECT";

            String userMessage = String.format(
                "Carrier Registration Details:\n" +
                "companyName: %s\n" +
                "email: %s\n" +
                "companyType: %s\n" +
                "registrationNumber: %s\n" +
                "licenseNumber: %s\n" +
                "taxId: %s\n" +
                "website: %s",
                carrier.getCompanyName(),
                carrier.getUser().getEmail(),
                carrier.getCompanyType(),
                carrier.getRegistrationNumber(),
                carrier.getLicenseNumber(),
                carrier.getTaxId(),
                carrier.getWebsite() != null ? carrier.getWebsite() : "not provided"
            );

            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(15000);

            RestClient restClient = restClientBuilder.requestFactory(factory).baseUrl(baseUrl).build();

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 400,
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
                )
            );

            String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String rawContent = jsonNode.path("choices").get(0).path("message").path("content").asText();

            // Extract JSON
            int start = rawContent.indexOf('{');
            int end = rawContent.lastIndexOf('}');
            String cleanJson = (start >= 0 && end > start) ? rawContent.substring(start, end + 1) : rawContent;

            ObjectMapper lenient = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
            JsonNode result = lenient.readTree(cleanJson);

            double riskScore = result.path("riskScore").asDouble(50.0);
            String riskStatus = result.path("riskStatus").asText("MEDIUM_RISK");
            String recommendation = result.path("recommendation").asText("MANUAL_REVIEW");

            carrier.setAiRiskScore(riskScore);
            carrier.setAiRiskStatus(riskStatus);
            carrier.setAiRecommendation(recommendation);

            log.info("Carrier {} AI validation: riskScore={}, status={}, recommendation={}",
                carrier.getCompanyName(), riskScore, riskStatus, recommendation);

            // Auto-approve disabled to enforce manual admin approval for all carriers
            /*
            if (riskScore < AUTO_APPROVE_THRESHOLD && "SAFE_TO_APPROVE".equalsIgnoreCase(recommendation)) {
                carrier.getUser().setUserStatus(com.tpa.enums.UserStatus.ACTIVE);
                log.info("Carrier {} auto-approved (riskScore < {})", carrier.getCompanyName(), AUTO_APPROVE_THRESHOLD);
            } else {
                log.info("Carrier {} requires manual admin review (riskScore={})", carrier.getCompanyName(), riskScore);
            }
            */
            log.info("Carrier {} requires manual admin review (riskScore={})", carrier.getCompanyName(), riskScore);

            carrierRepository.save(carrier);

        } catch (Exception e) {
            log.error("Carrier AI validation failed for {}: {}. Leaving INACTIVE for manual review.",
                carrier.getCompanyName(), e.getMessage());
            // On AI failure: set safe defaults and leave for manual review
            carrier.setAiRiskScore(50.0);
            carrier.setAiRiskStatus("MEDIUM_RISK");
            carrier.setAiRecommendation("MANUAL_REVIEW");
            carrierRepository.save(carrier);
        }
    }
}
