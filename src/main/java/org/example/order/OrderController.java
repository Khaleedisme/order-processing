package org.example.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order){
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.ok().body(createdOrder);
    }

    public ResponseEntity<Order> getOrder(@PathVariable Long id){
        Order order = orderService.findOrder(id);
        return  ResponseEntity.ok(order);
    }

}
