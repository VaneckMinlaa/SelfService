package com.banking.transaction.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtExtractor {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public Long extractUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            throw new RuntimeException("Jeton JWT manquant");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException e) {
            log.error("Échec de l'extraction de l'ID utilisateur du jeton: {}", e.getMessage());
            throw new RuntimeException("Jeton JWT invalide");
        }
    }

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
