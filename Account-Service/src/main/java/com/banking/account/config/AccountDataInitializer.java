package com.banking.account.config;

import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDataInitializer implements CommandLineRunner {
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) throws Exception {
        if (accountRepository.count() == 0) {
            log.info("Initializing demo accounts from Java code...");
            createAccount(1L, "FR7630001000200030004000501", "CHECKING", "50000.00");
            createAccount(2L, "FR7630001000200030004000505", "CHECKING", "1250000.00");
            createAccount(2L, "FR7630001000200030004000506", "SAVINGS", "4500000.00");
            createAccount(3L, "FR7630001000200030004000503", "CHECKING", "150000.00");
            createAccount(4L, "FR7630001000200030004000504", "CHECKING", "850000.00");
        }
    }

    private void createAccount(Long userId, String num, String type, String bal) {
        Account acc = new Account();
        acc.setUserId(userId); acc.setAccountNumber(num); acc.setAccountType(type);
        acc.setBalance(new BigDecimal(bal)); acc.setCurrency("XAF"); acc.setIsActive(true);
        accountRepository.save(acc);
    }
}
