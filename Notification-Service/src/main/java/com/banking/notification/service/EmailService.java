package com.banking.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${notification.from.email:noreply@banking.local}")
    private String fromEmailNotification;

    public void sendTransferNotification(String toEmail, String userName, String amount, String description) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailNotification);
            message.setTo(toEmail);
            message.setSubject("Banking Portal: Transfer Notification");
            message.setText(buildTransferEmailBody(userName, amount, description));

            mailSender.send(message);
            log.info("Transfer notification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendLoginNotification(String toEmail, String userName, String ipAddress) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailNotification);
            message.setTo(toEmail);
            message.setSubject("Banking Portal: Login Alert");
            message.setText(buildLoginEmailBody(userName, ipAddress));

            mailSender.send(message);
            log.info("Login notification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send login notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildTransferEmailBody(String userName, String amount, String description) {
        return String.format(
                "Dear %s,\n\n" +
                        "A transfer has been completed on your account.\n\n" +
                        "Amount: %s FCFA\n" +
                        "Description: %s\n" +
                        "Date: %s\n\n" +
                        "If you did not authorize this transfer, please contact our support team immediately.\n\n" +
                        "Best regards,\n" +
                        "Banking Portal Team",
                userName,
                amount,
                description,
                java.time.LocalDateTime.now()
        );
    }

    private String buildLoginEmailBody(String userName, String ipAddress) {
        return String.format(
                "Dear %s,\n\n" +
                        "A login to your Banking Portal account has been detected.\n\n" +
                        "IP Address: %s\n" +
                        "Date/Time: %s\n\n" +
                        "If this was not you, please contact our support team immediately.\n\n" +
                        "Best regards,\n" +
                        "Banking Portal Team",
                userName,
                ipAddress,
                java.time.LocalDateTime.now()
        );
    }
}
