package com.tpa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.response.DocumentValidationResponse;
import com.tpa.entity.Claim;
import com.tpa.entity.ClaimDocument;
import com.tpa.enums.ClaimStatus;
import com.tpa.repository.ClaimDocumentRepository;
import com.tpa.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiClaimAssistantServiceImplTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private ClaimDocumentRepository claimDocumentRepository;
    @Mock private RestClient.Builder restClientBuilder;
    @org.mockito.Spy private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private AiClaimAssistantServiceImpl aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "baseUrl", "http://localhost");
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "model", "test-model");

        when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    private void mockAiResponse(String jsonResponse) {
        String openaiResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"content\": \"" + jsonResponse.replace("\"", "\\\"").replace("\n", "\\n") + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        when(responseSpec.body(String.class)).thenReturn(openaiResponse);
    }

    @Test
    @DisplayName("TC-051 & TC-054: Validate valid doc and JSON parsing")
    void validateDocument_shouldReturnValidResponse_whenJsonIsClean() {
        String cleanJson = "{ \"status\": \"VALID\", \"confidenceScore\": 95, \"issues\": [] }";
        mockAiResponse(cleanJson);

        try (org.mockito.MockedStatic<org.apache.pdfbox.Loader> mockedLoader = mockStatic(org.apache.pdfbox.Loader.class);
             org.mockito.MockedConstruction<org.apache.pdfbox.text.PDFTextStripper> mockedStripper = mockConstruction(org.apache.pdfbox.text.PDFTextStripper.class, (mock, context) -> {
                 when(mock.getText(any())).thenReturn("Mocked PDF Content");
             })) {
            org.apache.pdfbox.pdmodel.PDDocument mockDoc = org.mockito.Mockito.mock(org.apache.pdfbox.pdmodel.PDDocument.class);
            mockedLoader.when(() -> org.apache.pdfbox.Loader.loadPDF(any(byte[].class))).thenReturn(mockDoc);
            
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
            DocumentValidationResponse response = aiService.validateDocument(file, "CLAIM_FORM");

            assertThat(response.getStatus()).isEqualTo("VALID");
            assertThat(response.getConfidenceScore()).isEqualTo(95);
            assertThat(response.getIssues()).isEmpty();
        }
    }

    @Test
    @DisplayName("TC-052 & TC-055: Validate invalid doc, No raw text")
    void validateDocument_shouldReturnInvalidResponse_andExtractJson_whenResponseHasMarkdown() {
        String dirtyJson = "Here is the result:\n```json\n{ \"status\": \"INVALID\", \"confidenceScore\": 40, \"issues\": [\"Missing signature\"] }\n```";
        mockAiResponse(dirtyJson);

        try (org.mockito.MockedStatic<org.apache.pdfbox.Loader> mockedLoader = mockStatic(org.apache.pdfbox.Loader.class);
             org.mockito.MockedConstruction<org.apache.pdfbox.text.PDFTextStripper> mockedStripper = mockConstruction(org.apache.pdfbox.text.PDFTextStripper.class, (mock, context) -> {
                 when(mock.getText(any())).thenReturn("Mocked PDF Content");
             })) {
            org.apache.pdfbox.pdmodel.PDDocument mockDoc = org.mockito.Mockito.mock(org.apache.pdfbox.pdmodel.PDDocument.class);
            mockedLoader.when(() -> org.apache.pdfbox.Loader.loadPDF(any(byte[].class))).thenReturn(mockDoc);

            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
            DocumentValidationResponse response = aiService.validateDocument(file, "CLAIM_FORM");

            assertThat(response.getStatus()).isEqualTo("INVALID");
            assertThat(response.getConfidenceScore()).isEqualTo(40);
            assertThat(response.getIssues()).containsExactly("Missing signature");
        }
    }

    @Test
    @DisplayName("TC-053: AI fallback on API failure")
    void validateDocument_shouldReturnFallback_whenApiFails() {
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Connection timeout"));

        try (org.mockito.MockedStatic<org.apache.pdfbox.Loader> mockedLoader = mockStatic(org.apache.pdfbox.Loader.class);
             org.mockito.MockedConstruction<org.apache.pdfbox.text.PDFTextStripper> mockedStripper = mockConstruction(org.apache.pdfbox.text.PDFTextStripper.class, (mock, context) -> {
                 when(mock.getText(any())).thenReturn("Mocked PDF Content");
             })) {
            org.apache.pdfbox.pdmodel.PDDocument mockDoc = org.mockito.Mockito.mock(org.apache.pdfbox.pdmodel.PDDocument.class);
            mockedLoader.when(() -> org.apache.pdfbox.Loader.loadPDF(any(byte[].class))).thenReturn(mockDoc);

            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
            DocumentValidationResponse response = aiService.validateDocument(file, "CLAIM_FORM");

            assertThat(response.getStatus()).isEqualTo("INVALID");
            assertThat(response.getConfidenceScore()).isEqualTo(0);
            assertThat(response.getIssues().get(0)).contains("AI validation service failed");
        }
    }

    @Test
    @DisplayName("TC-056: Confidence score validation")
    void validateDocument_shouldCapConfidenceScore() {
        String json = "{ \"status\": \"VALID\", \"confidenceScore\": 150, \"issues\": [] }";
        mockAiResponse(json);

        try (org.mockito.MockedStatic<org.apache.pdfbox.Loader> mockedLoader = mockStatic(org.apache.pdfbox.Loader.class);
             org.mockito.MockedConstruction<org.apache.pdfbox.text.PDFTextStripper> mockedStripper = mockConstruction(org.apache.pdfbox.text.PDFTextStripper.class, (mock, context) -> {
                 when(mock.getText(any())).thenReturn("Mocked PDF Content");
             })) {

            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
            DocumentValidationResponse response = aiService.validateDocument(file, "CLAIM_FORM");

            assertThat(response.getConfidenceScore()).isEqualTo(100); // Expecting bounded logic to cap at 100
        }
    }
}
