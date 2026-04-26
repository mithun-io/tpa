package com.tpa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.tpa.entity.Claim;
import com.tpa.repository.ClaimRepository;
import com.tpa.dto.request.AiValidationRequest;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.enums.Verdict;
import com.tpa.service.AiClaimAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.tpa.dto.response.DocumentValidationResponse;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClaimAssistantServiceImpl implements AiClaimAssistantService {

    private final ClaimRepository claimRepository;
    private final com.tpa.repository.ClaimDocumentRepository claimDocumentRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String model;

    @Override
    @Cacheable(value = "aiSummaries", key = "#claimId")
    public AiAnalysisResponse analyzeClaim(Long claimId, String prompt) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new RuntimeException("Claim not found"));

        List<com.tpa.entity.ClaimDocument> documents = claimDocumentRepository.findByClaimId(claimId);
        
        StringBuilder docContext = new StringBuilder();
        String aiValidationStatus = "UNKNOWN";
        String aiValidationIssues = "[]";
        Integer aiConfidenceScore = 0;

        for (com.tpa.entity.ClaimDocument doc : documents) {
            docContext.append("- ").append(doc.getType() != null ? doc.getType().name() : "UNKNOWN").append("\n");
            if (doc.getValidationStatus() != null) {
                aiValidationStatus = doc.getValidationStatus();
                aiValidationIssues = doc.getValidationIssues() != null ? doc.getValidationIssues() : "[]";
                aiConfidenceScore = doc.getConfidenceScore() != null ? doc.getConfidenceScore() : 0;
            }
        }

        String claimJson = String.format("{\"id\": %d, \"patientName\": \"%s\", \"hospitalName\": \"%s\", \"policyNumber\": \"%s\", \"status\": \"%s\", \"amount\": %f, \"admissionDate\": \"%s\", \"dischargeDate\": \"%s\"}",
                claim.getId(), safe(claim.getPatientName()), safe(claim.getHospitalName()), claim.getPolicyNumber(), claim.getStatus(), claim.getAmount(), safe(String.valueOf(claim.getAdmissionDate())), safe(String.valueOf(claim.getDischargeDate())));

        String validationJson = String.format("{\"status\": \"%s\", \"confidenceScore\": %d, \"issues\": %s}", 
                aiValidationStatus, aiConfidenceScore, aiValidationIssues);

        String systemPrompt = "You are an AI Claim Assistant integrated into an Insurance Claim Processing system.\n\n" +
                "Your job is to analyze a claim using the FULL CONTEXT provided. Do NOT ask for documents if they are already uploaded.\n\n" +
                "--- CONTEXT PROVIDED ---\n" +
                "Claim Details: " + claimJson + "\n" +
                "Uploaded Documents: \n" + docContext.toString() + "\n" +
                "AI Validation Result: " + validationJson + "\n\n" +
                "--- STRICT RULES ---\n" +
                "1. DO NOT ask for additional documents if claim_form or combined_document is uploaded.\n" +
                "2. DO NOT give generic responses like 'please upload documents'.\n" +
                "3. ALWAYS use the validation result provided.\n\n" +
                "--- YOUR TASK ---\n" +
                "Analyze and respond based on validation issues, claim data consistency, and risk indicators.\n\n" +
                "--- RESPONSE FORMAT ---\n" +
                "Return a clear, professional explanation containing:\n" +
                "1. Summary (what the claim is about)\n" +
                "2. Validation Result (VALID / INVALID)\n" +
                "3. Key Issues (if any)\n" +
                "4. Recommendation\n\n" +
                "CRITICAL INSTRUCTION: Return ONLY a valid JSON object matching this EXACT schema. Put your full, human-friendly response inside the 'recommendation' field.\n" +
                "{\n" +
                "  \"verdict\": \"REVIEW\",\n" +
                "  \"confidence\": 0.0,\n" +
                "  \"riskScore\": 0.0,\n" +
                "  \"validations\": {\"policyActive\": true, \"documentsComplete\": true, \"withinLimit\": true},\n" +
                "  \"financial\": {\"claimedAmount\": 0.0, \"eligibleAmount\": 0.0},\n" +
                "  \"flags\": [],\n" +
                "  \"recommendation\": \"<YOUR HUMAN FRIENDLY ANALYSIS HERE>\"\n" +
                "}";

        try {
            String rawAiContent = callAiApi(systemPrompt, prompt);
            log.info("Raw AI Response for claim {}:\n{}", claimId, rawAiContent);

            String cleanJson = extractJson(rawAiContent);
            AiAnalysisResponse analysis = buildLenientMapper().readValue(cleanJson, AiAnalysisResponse.class);

            if (analysis.getConfidence() < 0 || analysis.getConfidence() > 1) analysis.setConfidence(0.5);
            if (analysis.getRiskScore() < 0 || analysis.getRiskScore() > 1) analysis.setRiskScore(0.5);
            if (analysis.getFlags() == null) analysis.setFlags(new ArrayList<>());
            analysis.setGeneratedAt(LocalDateTime.now());

            return analysis;

        } catch (Exception e) {
            log.error("AI analysis failed for claim {}. Error: {}", claimId, e.getMessage(), e);
            return buildFallback(Verdict.REVIEW,
                    List.of("AI analysis failed: " + e.getMessage()),
                    "Manual review required due to AI failure");
        }
    }

    @Override
    public String generateClaimSummary(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // If summary already exists and hasn't changed drastically, we could return it.
        // But the prompt wants a "Regenerate" button, so we always generate here.
        String claimContext = String.format(
                "Patient: %s, Hospital: %s, Diagnosis: %s, Amount: %s, Policy: %s, Status: %s, Admission: %s, Discharge: %s",
                safe(claim.getPatientName()), safe(claim.getHospitalName()), safe(claim.getDiagnosis()),
                claim.getAmount(), safe(claim.getPolicyNumber()), claim.getStatus(),
                safe(claim.getAdmissionDate() != null ? claim.getAdmissionDate().toString() : ""),
                safe(claim.getDischargeDate() != null ? claim.getDischargeDate().toString() : "")
        );

        String systemPrompt = "You are a concise insurance summarization AI.\n" +
                "Summarize this claim in EXACTLY 3 short lines in simple English.\n" +
                "Include the patient name, hospital, issue/diagnosis, claimed amount, and the reasoning for its current risk or status.\n" +
                "Do not use markdown lists or bullet points. Just return 3 plain text sentences separated by a newline.\n\n" +
                "Claim Details:\n" + claimContext;

        try {
            String summary = callAiApi(systemPrompt, "Provide the 3-line summary.");
            // Clean up the text
            summary = summary.trim().replace("\"", "");
            
            // Persist the summary
            claim.setAiSummary(summary);
            claimRepository.save(claim);
            return summary;
        } catch (Exception e) {
            log.error("Failed to generate AI summary for claim {}", claimId, e);
            return "Unable to generate AI summary at this time.";
        }
    }

    // ─── AI Pre-Validation (stateless – no DB write) ───────────────────────────

    @Override
    public AiAnalysisResponse validatePreClaim(AiValidationRequest request) {
        log.info("Pre-validation AI request: policy={}, amount={}", request.getPolicyNumber(), request.getAmount());

        // ── Lightweight static rule checks BEFORE calling AI ──────────────────
        List<String> earlyFlags = new ArrayList<>();

        if (request.getPolicyNumber() == null || request.getPolicyNumber().isBlank()) {
            earlyFlags.add("Policy number is missing");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            earlyFlags.add("Claimed amount must be greater than zero");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            earlyFlags.add("Claimed amount exceeds maximum threshold of $50,000");
        }
        if (request.getHospitalName() == null || request.getHospitalName().isBlank()) {
            earlyFlags.add("Hospital name is missing");
        }

        // Short-circuit: fail hard for mandatory missing fields
        if (earlyFlags.stream().anyMatch(f -> f.contains("missing"))) {
            return buildFallback(Verdict.REVIEW, earlyFlags, "Please complete all required fields before AI validation");
        }

        // ── Build rich prompt for pre-validation context ──────────────────────
        String claimContext = String.format(
                "{\"policyNumber\": \"%s\", \"claimedAmount\": %s, \"hospitalName\": \"%s\", " +
                "\"diagnosis\": \"%s\", \"patientName\": \"%s\", \"admissionDate\": \"%s\", \"dischargeDate\": \"%s\"}",
                safe(request.getPolicyNumber()), request.getAmount(),
                safe(request.getHospitalName()), safe(request.getDiagnosis()),
                safe(request.getPatientName()), safe(request.getAdmissionDate()),
                safe(request.getDischargeDate())
        );

        String systemPrompt =
                "You are a TPA insurance pre-validation AI. A customer is ABOUT TO SUBMIT a claim. " +
                "Your job is to pre-validate the provided details and flag any potential issues BEFORE the claim is officially filed.\n\n" +
                "Claim details to validate:\n" + claimContext + "\n\n" +
                "Rules to evaluate:\n" +
                "1. Assess if the policy number format looks valid.\n" +
                "2. Check if the claimed amount is reasonable for the stated diagnosis.\n" +
                "3. Flag if hospital name is generic or suspicious.\n" +
                "4. Assess if admission/discharge dates are logically consistent.\n" +
                "5. Identify any missing critical fields.\n\n" +
                "CRITICAL INSTRUCTION: Return ONLY a valid JSON object. " +
                "NO markdown, NO code blocks, NO explanations, NO extra text whatsoever.\n" +
                "The response must be a raw JSON object that exactly matches this schema:\n" +
                "{\n" +
                "  \"verdict\": \"APPROVED\" | \"REVIEW\" | \"REJECTED\",\n" +
                "  \"confidence\": 0.0,\n" +
                "  \"riskScore\": 0.0,\n" +
                "  \"validations\": {\"policyActive\": true, \"documentsComplete\": true, \"withinLimit\": true},\n" +
                "  \"financial\": {\"claimedAmount\": 0.0, \"eligibleAmount\": 0.0},\n" +
                "  \"flags\": [],\n" +
                "  \"recommendation\": \"...\"\n" +
                "}";

        try {
            String rawAiContent = callAiApi(systemPrompt,
                    "Pre-validate this claim and return ONLY the JSON response, no other text.");

            log.info("Raw pre-validation AI response:\n{}", rawAiContent);

            String cleanJson = extractJson(rawAiContent);
            AiAnalysisResponse analysis = buildLenientMapper().readValue(cleanJson, AiAnalysisResponse.class);

            // Merge early static flags into AI flags
            if (!earlyFlags.isEmpty()) {
                List<String> merged = new ArrayList<>(earlyFlags);
                if (analysis.getFlags() != null) merged.addAll(analysis.getFlags());
                analysis.setFlags(merged);
            } else if (analysis.getFlags() == null) {
                analysis.setFlags(new ArrayList<>());
            }

            // Bounds enforcement
            if (analysis.getConfidence() < 0 || analysis.getConfidence() > 1) analysis.setConfidence(0.5);
            if (analysis.getRiskScore() < 0 || analysis.getRiskScore() > 1) analysis.setRiskScore(0.5);

            analysis.setGeneratedAt(LocalDateTime.now());
            return analysis;

        } catch (Exception e) {
            log.error("Pre-validation AI failed: {}", e.getMessage(), e);
            List<String> flags = new ArrayList<>(earlyFlags);
            flags.add("AI pre-validation failed: " + e.getMessage());
            return buildFallback(Verdict.REVIEW, flags, "AI pre-validation unavailable. Please proceed with manual review.");
        }
    }

    // ─── AI Document Validation ───────────────────────────────────────────────

    @Override
    public DocumentValidationResponse validateDocument(MultipartFile file, String documentType) {
        log.info("Validating document of type: {}", documentType);
        String extractedText;
        
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            extractedText = pdfStripper.getText(document);
        } catch (Exception e) {
            log.error("Failed to extract text from PDF", e);
            return DocumentValidationResponse.builder()
                    .status("INVALID")
                    .issues(List.of("Could not read PDF file: " + e.getMessage()))
                    .confidenceScore(0)
                    .build();
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            return DocumentValidationResponse.builder()
                    .status("INVALID")
                    .issues(List.of("The PDF appears to be empty or contains no extractable text"))
                    .confidenceScore(100)
                    .build();
        }

        String systemPrompt = "You are an AI document validator for an insurance TPA.\n" +
                "You are reviewing a document of type: " + documentType + "\n\n" +
                "Extracted Text:\n" + extractedText + "\n\n" +
                "Validate the document based on these rules:\n" +
                "1. Check for missing mandatory fields (Patient Name, Policy Number, Dates, Amounts).\n" +
                "2. Check for logical inconsistencies (e.g., Discharge date before Admission date).\n" +
                "3. Look for fraud indicators or suspicious text anomalies.\n" +
                "4. Check if amounts logically sum up (if applicable).\n\n" +
                "Return ONLY valid JSON. Issues must be short, clear, user-friendly sentences. Do not include technical terms, java package names, or stack traces.\n" +
                "Schema MUST be exactly:\n" +
                "{\n" +
                "  \"status\": \"VALID\" | \"INVALID\",\n" +
                "  \"issues\": [\"Discharge date must be after admission date\", \"Policy number format looks incorrect\"],\n" +
                "  \"confidenceScore\": 85\n" +
                "}";

        try {
            String rawAiContent = callAiApi(systemPrompt, "Validate this document and return JSON only.");
            log.info("Document AI Validation Result:\n{}", rawAiContent);

            String cleanJson = extractJson(rawAiContent);
            DocumentValidationResponse response = buildLenientMapper().readValue(cleanJson, DocumentValidationResponse.class);

            if (response.getIssues() == null) response.setIssues(new ArrayList<>());
            if (response.getConfidenceScore() == null) response.setConfidenceScore(50);
            if (response.getConfidenceScore() > 100) response.setConfidenceScore(100);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("AI Document Validation failed", e);
            return DocumentValidationResponse.builder()
                    .status("INVALID")
                    .issues(List.of("AI validation service failed: " + e.getMessage()))
                    .confidenceScore(0)
                    .build();
        }
    }

    // ─── Shared Helpers ───────────────────────────────────────────────────────

    /**
     * Makes the raw LLM API call and returns the content string from the response.
     */
    private String callAiApi(String systemPrompt, String userMessage) throws Exception {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        RestClient restClient = restClientBuilder.requestFactory(factory).baseUrl(baseUrl).build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 800,
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
        return jsonNode.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * Robustly extracts the first complete JSON object from a string,
     * even if the LLM wraps it in markdown or leading/trailing text.
     */
    private String extractJson(String raw) {
        if (raw == null) throw new RuntimeException("AI returned null response");
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            throw new RuntimeException("No valid JSON object found in AI response");
        }
        return raw.substring(start, end + 1);
    }

    /** Creates a lenient ObjectMapper tolerant of unknown fields and case-insensitive enums. */
    private ObjectMapper buildLenientMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
    }

    /** Builds a safe fallback AiAnalysisResponse. */
    private AiAnalysisResponse buildFallback(Verdict verdict, List<String> flags, String recommendation) {
        return AiAnalysisResponse.builder()
                .verdict(verdict)
                .confidence(0.5)
                .riskScore(0.5)
                .validations(AiAnalysisResponse.ValidationChecks.builder()
                        .policyActive(false).documentsComplete(false).withinLimit(false).build())
                .financial(AiAnalysisResponse.FinancialSummary.builder()
                        .claimedAmount(BigDecimal.ZERO).eligibleAmount(BigDecimal.ZERO).build())
                .flags(flags)
                .recommendation(recommendation)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /** Null-safe string helper for prompt construction. */
    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
