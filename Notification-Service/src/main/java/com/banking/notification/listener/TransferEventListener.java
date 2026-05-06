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
    private final com.banking.notification.service.SmsService smsService;
    private final com.banking.notification.client.AuthServiceClient authServiceClient;

    @RabbitListener(queues = "transfer-completed-queue")
    public void handleTransferCompleted(TransferEvent event) {
        try {
            log.info("Received transfer event for transaction: {}", event.getTransactionId());

            com.banking.notification.client.AuthServiceClient.UserInfo fromUser = authServiceClient.getUserInfo(event.getFromUserId());
            com.banking.notification.client.AuthServiceClient.UserInfo toUser = authServiceClient.getUserInfo(event.getToUserId());

            if (fromUser != null) {
                String fromUserName = fromUser.getFirstName() + " " + fromUser.getLastName();
                emailService.sendTransferNotification(
                        fromUser.getEmail(),
                        fromUserName,
                        event.getAmount().toString(),
                        "Outgoing transfer: " + event.getDescription()
                );
                
                String smsMsg = String.format("Bank Info: You have sent %s FCFA. Ref: %s", event.getAmount().toString(), event.getDescription());
                smsService.sendTransferSms(fromUser.getPhoneNumber(), smsMsg);
            }

            if (toUser != null) {
                String toUserName = toUser.getFirstName() + " " + toUser.getLastName();
                emailService.sendTransferNotification(
                        toUser.getEmail(),
                        toUserName,
                        event.getAmount().toString(),
                        "Incoming transfer: " + event.getDescription()
                );

                String smsMsg = String.format("Bank Info: You have received %s FCFA. Ref: %s", event.getAmount().toString(), event.getDescription());
                smsService.sendTransferSms(toUser.getPhoneNumber(), smsMsg);
            }

            log.info("Transfer notifications sent for transaction {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transfer event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
