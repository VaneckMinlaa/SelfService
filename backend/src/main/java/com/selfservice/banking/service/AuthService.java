package com.selfservice.banking.service;

import com.selfservice.banking.entity.User;
import com.selfservice.banking.repository.UserRepository;
import com.selfservice.banking.security.AuthAuditLogger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditLogger auditLogger;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            auditLogger.logLoginFailure(username, "user_not_found");
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLogger.logLoginFailure(username, "invalid_password");
            return null;
        }
        auditLogger.logLoginSuccess(username);
        return user;
    }
}
