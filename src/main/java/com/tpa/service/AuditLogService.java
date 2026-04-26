package com.tpa.service;

import com.tpa.enums.ClaimStatus;

public interface AuditLogService {
    void logAction(Long claimId, String action, ClaimStatus previousStatus, ClaimStatus newStatus);
}
