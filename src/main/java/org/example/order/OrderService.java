package org.example.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing-key.inventory}")
    private String inventoryRoutingKey;

    @Value("${rabbitmq.routing-key.inventory-rollback}")
    private String inventoryRollbackKey;

    public Order createOrder(Order order){
        Order savedOrder = orderRepository.save(order);
        log.info("Sifariş yaradıldı: id={}, status={}", savedOrder.getId(), savedOrder.getOrderStatus());

        OrderEvent event= new OrderEvent(
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount(),
                savedOrder.getCustomerId(),
                "ORDER_CREATED"
        );

        rabbitTemplate.convertAndSend(exchange, inventoryRoutingKey, event);
        log.info("OrderEvent göndərildi: orderId={}, routingKey={}", savedOrder.getId(), inventoryRoutingKey);
        return savedOrder;
    }

    @Transactional
    public void confirmPayment(Long orderId){
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PAYMENT_CONFIRMED);
        orderRepository.save(order);
        log.info("Ödəniş təsdiqləndi: orderId={}", orderId);
    }

    @Transactional
    public void confirmInventory(Long orderId){
        Order order = new Order();
        order.setOrderStatus(OrderStatus.INVENTORY_CONFIRMED);
        orderRepository.save(order);
        log.info("Inventory təsdiqləndi: orderId={}", orderId);
    }

    @Transactional
    public void completeOrder(Long orderId){
        Order order = new Order();
        order.setOrderStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        log.info("Sifariş tamamlandı: orderId={}", orderId);
    }

    @Transactional
    public void handlePaymentFailure(Long orderId, String productId, Integer quantity){
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        log.warn("Ödəniş uğursuz oldu: orderId={}", orderId);

        OrderEvent event = new OrderEvent(
                orderId,
                productId,
                quantity,
                null,
                null,
                "INVENTORY_ROLBACK"
        );

        rabbitTemplate.convertAndSend(exchange, inventoryRollbackKey, event);
        log.warn("Rollback event göndərildi: orderId={}, routingKey={}", orderId, inventoryRollbackKey);
    }

    @Transactional
    public void cancelOrder(Long orderId){
        Order order = findOrder(orderId);
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Sifariş ləğv edildi: orderId={}", orderId);
    }

    public Order findOrder(Long orderId){
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order tapılmadı."));
    }
}
