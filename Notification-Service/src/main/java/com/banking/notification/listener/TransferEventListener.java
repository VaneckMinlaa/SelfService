package com.banking.notification.listener;

import com.banking.notification.event.TransferEvent;
import com.banking.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = "transfer-completed-queue")
    public void handleTransferCompleted(TransferEvent event) {
        try {
            log.info("Received transfer event for transaction: {}", event.getTransactionId());

            // Note: In a real system, you would fetch user details from Auth Service
            // For now, just log the event
            String fromUserEmail = "from_user_" + event.getFromUserId() + "@example.com";
            String toUserEmail = "to_user_" + event.getToUserId() + "@example.com";
            String fromUserName = "User " + event.getFromUserId();
            String toUserName = "User " + event.getToUserId();

            // Send email to both parties
            emailService.sendTransferNotification(
                    fromUserEmail,
                    fromUserName,
                    event.getAmount().toString(),
                    "Outgoing transfer: " + event.getDescription()
            );

            emailService.sendTransferNotification(
                    toUserEmail,
                    toUserName,
                    event.getAmount().toString(),
                    "Incoming transfer: " + event.getDescription()
            );

            log.info("Transfer notifications sent for transaction {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transfer event: {}", e.getMessage(), e);
            // In production, you might want to implement retry logic here
        }
    }
}
