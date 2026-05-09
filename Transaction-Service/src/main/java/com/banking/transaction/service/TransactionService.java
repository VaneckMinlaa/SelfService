package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionDTO;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.TransferEvent;
import com.banking.transaction.producer.TransferEventProducer;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransferEventProducer transferEventProducer;
    private final RestTemplate restTemplate;

    @Transactional
    public TransactionDTO deposit(Long accountId, BigDecimal amount, Long userId) {
        log.debug("Deposit initiated: account {}, amount {}", accountId, amount);
        String accountServiceUrl = System.getenv().getOrDefault("ACCOUNT_SERVICE_URL", "http://localhost:8082");

        restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + accountId + "/credit?amount=" + amount,
                org.springframework.http.HttpMethod.PUT, null, Void.class);

        Transaction transaction = new Transaction();
        transaction.setToAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setTransactionType("DEPOSIT");
        transaction.setDescription("Dépôt sur compte");
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);

        TransferEvent event = new TransferEvent();
        event.setTransactionId(saved.getId());
        event.setToUserId(userId);
        event.setToAccountId(accountId);
        event.setAmount(amount);
        event.setDescription("Dépôt effectué");
        event.setTimestamp(LocalDateTime.now());
        transferEventProducer.publishTransferCompleted(event);

        return convertToDTO(saved);
    }

    @Transactional
    public TransactionDTO withdraw(Long accountId, BigDecimal amount, Long userId) {
        log.debug("Withdrawal initiated: account {}, amount {}", accountId, amount);
        String accountServiceUrl = System.getenv().getOrDefault("ACCOUNT_SERVICE_URL", "http://localhost:8082");

        restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + accountId + "/debit?amount=" + amount + "&userId=" + userId,
                org.springframework.http.HttpMethod.PUT, null, Void.class);

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(accountId);
        transaction.setAmount(amount.negate());
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setDescription("Retrait d'argent");
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);

        TransferEvent event = new TransferEvent();
        event.setTransactionId(saved.getId());
        event.setFromUserId(userId);
        event.setFromAccountId(accountId);
        event.setAmount(amount.negate());
        event.setDescription("Retrait effectué");
        event.setTimestamp(LocalDateTime.now());
        transferEventProducer.publishTransferCompleted(event);

        return convertToDTO(saved);
    }

    public TransactionDTO transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                   String description, Long userId) {
        log.debug("Transfer initiated: from {} to {}, amount {}", fromAccountId, toAccountId, amount);

        // Validate inputs
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        String accountServiceUrl = System.getenv().getOrDefault("ACCOUNT_SERVICE_URL", "http://localhost:8082");

        // 1. Debit source account
        org.springframework.http.ResponseEntity<Void> debitResponse = restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + fromAccountId + "/debit?amount=" + amount + "&userId=" + userId,
                org.springframework.http.HttpMethod.PUT,
                null,
                Void.class
        );

        if (!debitResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors du debit du compte source");
        }

        // 2. Credit destination account
        org.springframework.http.ResponseEntity<Void> creditResponse = restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + toAccountId + "/credit?amount=" + amount,
                org.springframework.http.HttpMethod.PUT,
                null,
                Void.class
        );

        if (!creditResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors du credit du compte destination");
        }

        // 3. Fetch destination account info to get toUserId for notification
        Long toUserId = null;
        try {
            org.springframework.http.ResponseEntity<Map> accountResponse = restTemplate.getForEntity(
                    accountServiceUrl + "/api/accounts/internal/" + toAccountId,
                    Map.class
            );
            if (accountResponse.getStatusCode().is2xxSuccessful() && accountResponse.getBody() != null) {
                toUserId = ((Number) accountResponse.getBody().get("userId")).longValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch destination user ID for notification: {}", e.getMessage());
        }

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setAmount(amount);
        transaction.setTransactionType("TRANSFER");
        transaction.setDescription(description);
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 4. Publish event for notification service
        TransferEvent event = new TransferEvent();
        event.setTransactionId(savedTransaction.getId());
        event.setFromUserId(userId);
        event.setToUserId(toUserId);
        event.setFromAccountId(fromAccountId);
        event.setToAccountId(toAccountId);
        event.setAmount(amount);
        event.setDescription(description);
        event.setTimestamp(LocalDateTime.now());

        transferEventProducer.publishTransferCompleted(event);

        // 5. Log to Audit Trail
        try {
            String authServiceUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8081");
            String details = String.format("Virement de %s XAF du compte %d vers le compte %d. Motif: %s", 
                amount.toString(), fromAccountId, toAccountId, description);
            
            restTemplate.postForEntity(
                authServiceUrl + "/api/auth/audit/log?userId=" + userId + 
                "&action=TRANSFER&ipAddress=INTERNAL&success=true&details=" + details,
                null, Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to send audit log for transfer: {}", e.getMessage());
        }

        log.info("Transfer completed: transaction {}", savedTransaction.getId());

        return convertToDTO(savedTransaction);
    }

    public Page<TransactionDTO> getAccountTransactions(Long accountId, Pageable pageable) {
        log.debug("Fetching all transactions for account: {}", accountId);
        Page<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId, pageable);
        return transactions.map(this::convertToDTO);
    }

    public TransactionDTO getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return convertToDTO(transaction);
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
