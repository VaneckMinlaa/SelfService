package com.banking.auth.service;

import com.banking.auth.dto.AuthResponse;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request, String clientIp) {
        log.debug("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Audit log failed attempt
        if (user == null || !user.getIsActive()) {
            auditService.log(null, "FAILED_LOGIN", clientIp, false,
                    "User not found or inactive: " + request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.log(user.getId(), "FAILED_LOGIN", clientIp, false,
                    "Invalid password for user: " + request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        // Generate token
        String token = jwtProvider.generateToken(user.getId(), user.getEmail());

        // Audit log successful login
        auditService.log(user.getId(), "LOGIN", clientIp, true,
                "User logged in successfully");

        log.info("User {} logged in successfully", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .expiresIn(jwtProvider.getExpirationTime())
                .build();
    }

    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }

    public Long getUserIdFromToken(String token) {
        return jwtProvider.getUserIdFromToken(token);
    }

    public User getUserById(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElse(null);
    }

    public void logout(Long userId, String clientIp) {
        auditService.log(userId, "LOGOUT", clientIp, true,
                "User logged out successfully");
        log.info("User {} logged out", userId);
    }
}
