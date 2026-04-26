package com.tpa.kafka.event;

import com.tpa.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimNotificationEvent {

    private Long claimId;

    private String policyNumber;

    private String customerEmail;

    private ClaimStatus status;

    private String message;
}
