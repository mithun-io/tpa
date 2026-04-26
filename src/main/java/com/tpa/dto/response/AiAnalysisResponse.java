package com.tpa.dto.response;

import com.tpa.enums.Verdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    private Verdict verdict;
    private double confidence;
    private double riskScore;

    private ValidationChecks validations;
    private FinancialSummary financial;

    private List<String> flags;
    private String recommendation;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationChecks {
        private boolean policyActive;
        private boolean documentsComplete;
        private boolean withinLimit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private BigDecimal claimedAmount;
        private BigDecimal eligibleAmount;
    }
}
