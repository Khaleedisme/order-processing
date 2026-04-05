package org.example.order;

import java.math.BigDecimal;

public record OrderEvent(
        Long orderId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount,
        String customerId,
        String eventType
) {
}
