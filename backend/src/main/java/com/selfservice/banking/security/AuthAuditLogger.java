package com.selfservice.banking.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Journalisation des connexions et accès sensibles (comptes, transactions).
 * Logs structurés pour collecte (ex. JSON via logback).
 */
@Component
public class AuthAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditLogger.class);

    public void logLoginSuccess(String username) {
        log.info("auth_login_success username={}", username);
    }

    public void logLoginFailure(String username, String reason) {
        log.warn("auth_login_failure username={} reason={}", username, reason);
    }

    public void logAccountListAccess(String username) {
        log.info("action=account_list_access username={}", username);
    }

    public void logAccountDetailAccess(String username, Long accountId, String iban) {
        log.info("action=account_detail_access username={} accountId={} iban={}", username, accountId, iban);
    }

    public void logTransactionListAccess(String username, Long accountId, String iban) {
        log.info("action=transaction_list_access username={} accountId={} iban={}", username, accountId, iban);
    }

    public void logLogout(String username) {
        log.info("auth_logout username={}", username);
    }
}
