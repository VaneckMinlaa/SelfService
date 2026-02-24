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
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setIpAddress(ipAddress);
            log.setSuccess(success);
            log.setDetails(details);

            auditLogRepository.save(log);

            if (!success) {
                log.warn("Security event - Action: {}, User: {}, IP: {}, Details: {}",
                        action, userId, ipAddress, details);
            } else {
                log.debug("Audit logged - Action: {}, User: {}", action, userId);
            }
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    public List<AuditLog> getUserAuditLogs(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getFailedLoginAttempts(String action) {
        return auditLogRepository.findByActionAndSuccessFalse(action);
    }
}
