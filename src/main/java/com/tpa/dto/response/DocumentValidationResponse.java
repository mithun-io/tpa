package com.tpa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationResponse {
    private String status; // VALID or INVALID
    private List<String> issues;
    private Integer confidenceScore; // 0 to 100
}
