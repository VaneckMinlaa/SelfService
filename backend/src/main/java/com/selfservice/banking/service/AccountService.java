package com.selfservice.banking.service;

import com.selfservice.banking.dto.AccountDto;
import com.selfservice.banking.dto.TransactionDto;
import com.selfservice.banking.entity.Account;
import com.selfservice.banking.entity.Transaction;
import com.selfservice.banking.entity.User;
import com.selfservice.banking.repository.AccountRepository;
import com.selfservice.banking.repository.TransactionRepository;
import com.selfservice.banking.security.AuthAuditLogger;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final int TRANSACTIONS_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthAuditLogger auditLogger;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AuthAuditLogger auditLogger) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
    }

    public List<AccountDto> getAccountsForUser(User user) {
        auditLogger.logAccountListAccess(user.getUsername());
        List<Account> accounts = accountRepository.findByOwnerId(user.getId());
        return accounts.stream().map(this::toDto).collect(Collectors.toList());
    }

    public AccountDto getAccount(Long accountId, User user) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null || !account.getOwner().getId().equals(user.getId())) {
            return null;
        }
        auditLogger.logAccountDetailAccess(user.getUsername(), accountId, account.getIban());
        return toDto(account);
    }

    public List<TransactionDto> getTransactions(Long accountId, User user) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null || !account.getOwner().getId().equals(user.getId())) {
            return List.of();
        }
        auditLogger.logTransactionListAccess(user.getUsername(), accountId, account.getIban());
        return transactionRepository
                .findByAccountIdOrderByOccurredAtDesc(accountId, PageRequest.of(0, TRANSACTIONS_PAGE_SIZE))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AccountDto toDto(Account a) {
        AccountDto dto = new AccountDto();
        dto.setId(a.getId());
        dto.setIban(a.getIban());
        dto.setLabel(a.getLabel());
        dto.setBalance(a.getBalance());
        dto.setCurrency(a.getCurrency());
        return dto;
    }

    private TransactionDto toDto(Transaction t) {
        TransactionDto dto = new TransactionDto();
        dto.setId(t.getId());
        dto.setOccurredAt(t.getOccurredAt());
        dto.setDescription(t.getDescription());
        dto.setAmount(t.getAmount());
        dto.setBalanceAfter(t.getBalanceAfter());
        return dto;
    }
}
