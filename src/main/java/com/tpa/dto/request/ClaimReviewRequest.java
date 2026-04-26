package com.tpa.dto.request;

import com.tpa.enums.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewRequest {
    
    @NotNull(message = "Claim ID cannot be null")
    private Long claimId;

    @NotNull(message = "Status cannot be null")
    private ClaimStatus status;

    @NotBlank(message = "Review notes are required")
    private String reviewNotes;
}
