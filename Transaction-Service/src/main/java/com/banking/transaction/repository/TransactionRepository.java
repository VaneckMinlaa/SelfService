package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountId(Long fromAccountId);

    List<Transaction> findByToAccountId(Long toAccountId);

    Page<Transaction> findByFromAccountIdOrderByCreatedAtDesc(Long fromAccountId, Pageable pageable);

    Page<Transaction> findByToAccountIdOrderByCreatedAtDesc(Long toAccountId, Pageable pageable);

    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromAccountId, Long toAccountId, Pageable pageable);
}
