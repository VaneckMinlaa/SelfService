package com.banking.account.service;

import com.banking.account.dto.AccountDTO;
import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountDTO> getUserAccounts(Long userId) {
        log.debug("Fetching accounts for user: {}", userId);
        List<Account> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        return accounts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO getAccount(Long accountId, Long userId) {
        log.debug("Fetching account {} for user {}", accountId, userId);
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé or access denied"));

        return convertToDTO(account);
    }

    public void verifyAccountOwnership(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé"));

        if (!account.getUserId().equals(userId)) {
            log.warn("Accès refusé: User {} tried to access account {}", userId, accountId);
            throw new RuntimeException("Accès refusé");
        }
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé"));
    }

    private AccountDTO convertToDTO(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void debit(Long accountId, java.math.BigDecimal amount, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    @org.springframework.transaction.annotation.Transactional
    public void credit(Long accountId, java.math.BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    public AccountDTO getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account with number " + accountNumber + " not found"));
        return convertToDTO(account);
    }
}
