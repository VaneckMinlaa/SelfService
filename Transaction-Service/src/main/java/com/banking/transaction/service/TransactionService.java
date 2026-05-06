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

        // Debit source account
        org.springframework.http.ResponseEntity<Void> debitResponse = restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + fromAccountId + "/debit?amount=" + amount + "&userId=" + userId,
                org.springframework.http.HttpMethod.PUT,
                null,
                Void.class
        );

        if (!debitResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors du debit du compte source");
        }

        // Credit destination account
        org.springframework.http.ResponseEntity<Void> creditResponse = restTemplate.exchange(
                accountServiceUrl + "/api/accounts/" + toAccountId + "/credit?amount=" + amount,
                org.springframework.http.HttpMethod.PUT,
                null,
                Void.class
        );

        if (!creditResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors du credit du compte destination");
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

        // Publish event for notification service
        TransferEvent event = new TransferEvent();
        event.setTransactionId(savedTransaction.getId());
        event.setFromAccountId(fromAccountId);
        event.setToAccountId(toAccountId);
        event.setAmount(amount);
        event.setDescription(description);
        event.setTimestamp(LocalDateTime.now());

        transferEventProducer.publishTransferCompleted(event);

        log.info("Transfer completed: transaction {}", savedTransaction.getId());

        return convertToDTO(savedTransaction);
    }

    public Page<TransactionDTO> getAccountTransactions(Long accountId, Pageable pageable) {
        log.debug("Fetching transactions for account: {}", accountId);
        Page<Transaction> transactions = transactionRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId, pageable);
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
