package com.freshcart.controllers;

import com.freshcart.dto.PaymentVerificationRequest;
import com.freshcart.entities.Order;
import com.freshcart.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping("/MyOrders")
    public ResponseEntity<List<Order>> myOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName()));
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(Authentication authentication) {
        return ResponseEntity.ok(orderService.createRazorpayOrder(authentication.getName()));
    }

    @PostMapping("/cancel-payment/{razorpayOrderId}")
    public ResponseEntity<Void> cancelPayment(@PathVariable String razorpayOrderId, Authentication authentication) {
        orderService.cancelPayment(authentication.getName(), razorpayOrderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/place-order")
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody PaymentVerificationRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(orderService.placeOrder(authentication.getName(), request));
    }

    @DeleteMapping("/deleteItem/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id, Authentication authentication) {
        orderService.deleteItemId(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> ordersByUser(@PathVariable Integer userId, Authentication authentication) {
        // The authenticated user's username is used to prevent reading another customer's order history.
        if (!orderService.getUserId(authentication.getName()).equals(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }
}
