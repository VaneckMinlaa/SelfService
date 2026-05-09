package com.banking.transaction.config;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionDataInitializer implements CommandLineRunner {
    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (transactionRepository.count() == 0) {
            log.info("Initializing demo transactions from Java code...");
            createTransaction(2L, 3L, "50000.00", "TRANSFER", "Virement vers épargne", 48);
            createTransaction(3L, 2L, "10000.00", "TRANSFER", "Besoin de liquidités", 24);
            createTransaction(1L, 2L, "15000.00", "TRANSFER", "Remboursement dette", 5);
            createTransaction(2L, 4L, "2500.00", "TRANSFER", "Cadeau anniversaire", 2);
        }
    }

    private void createTransaction(Long from, Long to, String amount, String type, String desc, int hours) {
        Transaction tx = new Transaction();
        tx.setFromAccountId(from); tx.setToAccountId(to);
        tx.setAmount(new BigDecimal(amount)); tx.setTransactionType(type);
        tx.setDescription(desc); tx.setStatus("COMPLETED");
        tx.setCompletedAt(LocalDateTime.now().minusHours(hours));
        transactionRepository.save(tx);
    }
}
