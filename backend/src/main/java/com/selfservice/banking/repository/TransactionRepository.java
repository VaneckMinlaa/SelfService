package com.selfservice.banking.repository;

import com.selfservice.banking.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByOccurredAtDesc(Long accountId, Pageable pageable);
}
