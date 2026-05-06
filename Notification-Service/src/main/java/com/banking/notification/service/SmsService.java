package com.banking.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final RestTemplate restTemplate;
    
    private static final String BREVO_SMS_URL = "https://api.brevo.com/v3/transactionalSMS/sms";

    public void sendTransferSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.warn("Cannot send SMS: No phone number provided.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("type", "transactional");
            body.put("unicodeEnabled", false);
            body.put("sender", "BankPortal");
            body.put("recipient", phoneNumber);
            body.put("content", message);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_SMS_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to {}", phoneNumber);
            } else {
                log.error("Failed to send SMS to {}. Status: {}, Response: {}", phoneNumber, response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error sending SMS via Brevo: {}", e.getMessage(), e);
        }
    }
}
