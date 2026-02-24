package com.banking.transaction.producer;

import com.banking.transaction.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishTransferCompleted(TransferEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    "transfers-exchange",
                    "transfer.completed",
                    event
            );
            log.info("Transfer event published for transaction {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish transfer event: {}", e.getMessage(), e);
        }
    }
}
