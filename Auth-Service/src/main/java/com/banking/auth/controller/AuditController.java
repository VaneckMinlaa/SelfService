package com.banking.auth.controller;

import com.banking.auth.entity.AuditLog;
import com.banking.auth.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(auditService.getUserAuditLogs(userId));
    }

    @PostMapping("/log")
    public ResponseEntity<Void> createLog(
            @RequestParam Long userId,
            @RequestParam String action,
            @RequestParam String ipAddress,
            @RequestParam boolean success,
            @RequestParam String details) {
        auditService.log(userId, action, ipAddress, success, details);
        return ResponseEntity.ok().build();
    }
}
