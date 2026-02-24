package com.banking.auth.repository;

import com.banking.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    List<AuditLog> findByActionAndSuccessFalse(String action);
}
