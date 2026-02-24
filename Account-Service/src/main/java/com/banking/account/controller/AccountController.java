package com.banking.account.controller;

import com.banking.account.dto.AccountDTO;
import com.banking.account.service.AccountService;
import com.banking.account.util.JwtExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final JwtExtractor jwtExtractor;

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getUserAccounts(HttpServletRequest request) {
        try {
            Long userId = jwtExtractor.extractUserId(request);
            List<AccountDTO> accounts = accountService.getUserAccounts(userId);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error fetching accounts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = jwtExtractor.extractUserId(request);
            accountService.verifyAccountOwnership(id, userId);
            AccountDTO account = accountService.getAccount(id, userId);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Error fetching account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Account Service is healthy");
    }
}
