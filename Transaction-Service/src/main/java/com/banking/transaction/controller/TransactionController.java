package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionDTO;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.util.JwtExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtExtractor jwtExtractor;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = jwtExtractor.extractUserId(httpRequest);

            Long fromAccountId = ((Number) request.get("fromAccountId")).longValue();
            Long toAccountId = ((Number) request.get("toAccountId")).longValue();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String description = (String) request.getOrDefault("description", "");

            TransactionDTO transaction = transactionService.transfer(fromAccountId, toAccountId, amount, description, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            log.error("Error processing transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = jwtExtractor.extractUserId(request);
            Pageable pageable = PageRequest.of(page, size);
            // Note: In real system, get accountId from request param or path
            // For demo, we'll just return empty page
            return ResponseEntity.ok(Page.empty());
        } catch (Exception e) {
            log.error("Error fetching transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long userId = jwtExtractor.extractUserId(request);
            TransactionDTO transaction = transactionService.getTransaction(id);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            log.error("Error fetching transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is healthy");
    }
}
