package com.virtualclassroom.service;

import com.virtualclassroom.model.AuditLog;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditLogService.class);

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }


    @Transactional
    public void logAction(User actor, String action, String targetEntity, String targetId, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .actorEmail(actor != null ? actor.getEmail() : "ANONYMOUS")
                    .action(action)
                    .targetEntity(targetEntity)
                    .targetId(targetId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
