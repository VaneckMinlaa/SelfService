package com.banking.auth.service;

import com.banking.auth.entity.AuditLog;
import com.banking.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long userId, String action, String ipAddress, boolean success, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setIpAddress(ipAddress);
            auditLog.setSuccess(success);
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);

            if (!success) {
                AuditService.log.warn("Security event - Action: {}, User: {}, IP: {}, Details: {}",
                        action, userId, ipAddress, details);
            } else {
                AuditService.log.debug("Audit logged - Action: {}, User: {}", action, userId);
            }
        } catch (Exception e) {
            AuditService.log.error("Failed to save audit log", e);
        }
    }

    public List<AuditLog> getUserAuditLogs(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getFailedLoginAttempts(String action) {
        return auditLogRepository.findByActionAndSuccessFalse(action);
    }
}
