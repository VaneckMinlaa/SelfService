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

/**
 * Service d'authentification responsable de la gestion des utilisateurs et des jetons JWT.
 * Gère la connexion, la validation des jetons et la journalisation des événements de sécurité.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authentifie un utilisateur et génère un jeton JWT.
     *
     * @param request La requête de connexion contenant l'email et le mot de passe
     * @param clientIp L'adresse IP du client pour l'audit
     * @return La réponse d'authentification avec le jeton JWT
     * @throws RuntimeException Si les identifiants sont invalides
     */
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = request.getEmail().trim().toLowerCase();
        String rawPassword = request.getPassword().trim();
        
        log.info("Tentative de connexion pour l'email : {}", email);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null || !user.getIsActive()) {
            auditService.log(null, "FAILED_LOGIN", clientIp, false,
                    "Utilisateur non trouvé ou inactif: " + email);
            throw new RuntimeException("Identifiants invalides");
        }

        log.debug("Validation mot de passe pour {}. Longueur reçu: {}, Longueur hash en base: {}", 
                  email, rawPassword.length(), user.getPasswordHash().length());

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Échec de la comparaison BCrypt pour l'utilisateur {}", email);
            auditService.log(user.getId(), "FAILED_LOGIN", clientIp, false,
                    "Mot de passe invalide pour l'utilisateur: " + email);
            throw new RuntimeException("Identifiants invalides");
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail());

        auditService.log(user.getId(), "LOGIN", clientIp, true,
                "Utilisateur connecté avec succès");

        log.info("L'utilisateur {} s'est connecté avec succès", request.getEmail());

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
