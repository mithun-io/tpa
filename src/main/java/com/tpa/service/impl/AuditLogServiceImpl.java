package com.tpa.service.impl;

import com.tpa.entity.AuditLog;
import com.tpa.enums.ClaimStatus;
import com.tpa.repository.AuditLogRepository;
import com.tpa.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final com.tpa.repository.ClaimAuditRepository claimAuditRepository;
    private final com.tpa.repository.ClaimRepository claimRepository;

    @Override
    @Async("taskExecutor")
    public void logAction(Long claimId, String action, ClaimStatus previousStatus, ClaimStatus newStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String performedBy = (authentication != null && authentication.getName() != null) 
                ? authentication.getName() : "SYSTEM";

        // 1. Save to generic AuditLog
        AuditLog auditLog = AuditLog.builder()
                .claimId(claimId)
                .action(action)
                .previousStatus(previousStatus != null ? previousStatus.name() : null)
                .newStatus(newStatus != null ? newStatus.name() : null)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(auditLog);

        // 2. Save to specific ClaimAudit for Timeline
        if (newStatus != null) {
            claimRepository.findById(claimId).ifPresent(claim -> {
                com.tpa.entity.ClaimAudit claimAudit = com.tpa.entity.ClaimAudit.builder()
                        .claim(claim)
                        .previousStatus(previousStatus)
                        .newStatus(newStatus)
                        .changedBy(performedBy)
                        .notes(action.replace("_", " "))
                        .build();
                claimAuditRepository.save(claimAudit);
            });
        }
    }
}
