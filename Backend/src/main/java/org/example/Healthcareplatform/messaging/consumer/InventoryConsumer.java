package org.example.Healthcareplatform.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.messaging.config.RabbitMQConfig;
import org.example.Healthcareplatform.messaging.event.HealthcareEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order/payment events to log inventory deduction requests.
 * In a full implementation this would call InventoryService to deduct
 * or reserve stock; here it logs the action for audit visibility.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
    public void handle(HealthcareEvent event) {
        log.info("Inventory consumer — type={}, userId={}", event.getType(), event.getUserId());
        try {
            switch (event.getType()) {
                case RabbitMQConfig.RK_ORDER_CREATED -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    int itemCount = toInt(event.getData().get("itemCount"));
                    log.info("Inventory deduction requested — orderId={}, items={}", orderId, itemCount);
                    // TODO: call inventoryService.deductStock(orderId) when inventory is implemented
                }
                case RabbitMQConfig.RK_PAYMENT_SUCCESS -> {
                    Long orderId = toLong(event.getData().get("orderId"));
                    log.info("Inventory stock confirmed for paid order — orderId={}", orderId);
                }
                default -> log.debug("Inventory consumer ignoring type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Inventory consumer error — type={}, error={}", event.getType(), e.getMessage());
            throw e;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
