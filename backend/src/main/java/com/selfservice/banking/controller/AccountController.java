package com.selfservice.banking.controller;

import com.selfservice.banking.dto.AccountDto;
import com.selfservice.banking.dto.TransactionDto;
import com.selfservice.banking.entity.User;
import com.selfservice.banking.security.UserPrincipal;
import com.selfservice.banking.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = principal.getUser();
        return ResponseEntity.ok(accountService.getAccountsForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        AccountDto account = accountService.getAccount(id, principal.getUser());
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDto>> transactions(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<TransactionDto> list = accountService.getTransactions(id, principal.getUser());
        return ResponseEntity.ok(list);
    }
}
