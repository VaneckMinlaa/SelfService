package com.banking.notification.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://auth-service:8081}")
    private String authServiceUrl;

    public UserInfo getUserInfo(Long userId) {
        try {
            ResponseEntity<UserInfo> response = restTemplate.getForEntity(
                    authServiceUrl + "/api/auth/user/" + userId,
                    UserInfo.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch user info for userId: {}", userId, e);
        }
        return null;
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
    }
}
