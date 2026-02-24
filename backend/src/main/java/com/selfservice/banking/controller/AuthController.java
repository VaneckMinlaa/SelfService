package com.selfservice.banking.controller;

import com.selfservice.banking.dto.LoginRequest;
import com.selfservice.banking.dto.UserInfoDto;
import com.selfservice.banking.entity.User;
import com.selfservice.banking.service.AuthService;
import com.selfservice.banking.security.AuthAuditLogger;
import com.selfservice.banking.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthAuditLogger auditLogger;

    public AuthController(AuthService authService, AuthAuditLogger auditLogger) {
        this.authService = authService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = authService.authenticate(request.getUsername(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("user", user);
        UserInfoDto dto = new UserInfoDto();
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            auditLogger.logLogout(up.getUser().getUsername());
        }
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = principal.getUser();
        UserInfoDto dto = new UserInfoDto();
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        return ResponseEntity.ok(dto);
    }
}
