package com.freshcart.services;

import com.freshcart.entities.*;
import com.freshcart.exceptions.BadRequestException;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.OrderRepo;
import com.freshcart.repos.ProductRepo;
import com.freshcart.repos.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AdminService {
    private final ProductService productService;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final OrderRepo orderRepo;
    private final String razorpayKeyId;
    private final String razorpaySecret;

    public AdminService(ProductService productService, ProductRepo productRepo, UserRepo userRepo, OrderRepo orderRepo,
                        @Value("${app.razorpay.key-id:}") String razorpayKeyId,
                        @Value("${app.razorpay.key-secret:}") String razorpaySecret) {
        this.productService = productService;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpaySecret = razorpaySecret;
    }

    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalProducts", productRepo.countByActiveTrue());
        data.put("totalUsers", userRepo.countByActiveTrue());
        data.put("totalOrders", orderRepo.count());
        return data;
    }

    public List<Product> getAllProducts() { return productService.getAllProductsForAdmin(); }
    public Product addProduct(com.freshcart.dto.ProductRequest request) { return productService.create(request); }
    public Product updateProduct(Integer id, com.freshcart.dto.ProductRequest request) { return productService.update(id, request); }
    public void deleteProduct(Integer id) { productService.delete(id); }
    public List<User> getAllUsers() { return userRepo.findAll(); }

    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) throw new BadRequestException("Admin accounts cannot be deleted from this endpoint");
        user.setActive(false);
        user.setVerified(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepo.save(user);
    }

    public List<Order> getAllOrders() { return orderRepo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")); }

    @Transactional
    public Order cancelAndRefund(Integer id) {
        Order order = orderRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) return order;
        if (order.getStatus() == OrderStatus.DELIVERED) throw new BadRequestException("Delivered orders cannot be cancelled");
        if (order.getPaymentStatus() != PaymentStatus.PAID || order.getRazorpayPaymentId() == null) {
            throw new BadRequestException("This order does not have a refundable captured payment");
        }
        ensureRazorpayConfigured();
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            com.razorpay.Payment payment = client.payments.fetch(order.getRazorpayPaymentId());
            int amount = Integer.parseInt(responseValue(payment.get("amount")));
            int refunded = Integer.parseInt(responseValue(payment.get("amount_refunded")));
            if (refunded < amount) {
                client.payments.refund(order.getRazorpayPaymentId());
            }
        } catch (RazorpayException | NumberFormatException ex) {
            throw new BadRequestException("Unable to refund the Razorpay payment");
        }
        for (OrderItem item : order.getItems()) {
            Product product = productRepo.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product for order item not found"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepo.save(product);
        }
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepo.save(order);
    }

    private static String responseValue(Object value) {
        return Objects.toString(value, "");
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId.isBlank() || razorpaySecret.isBlank()) throw new BadRequestException("Razorpay is not configured");
    }

    @Transactional
    public Order updateOrderStatus(Integer id, OrderStatus newStatus) {
        Order order = orderRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus oldStatus = order.getStatus();
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new BadRequestException("Invalid order status transition: " + oldStatus + " -> " + newStatus);
        }
        if (newStatus == OrderStatus.CANCELLED && order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Paid orders require the refund workflow before cancellation");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepo.findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product for order item not found"));
                product.setStock(product.getStock() + item.getQuantity());
                productRepo.save(product);
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                // Money cannot be refunded by the database transaction. Keep the payment marked PAID
                // until a real Razorpay refund is initiated, rather than falsely claiming it was refunded.
            }
        }
        if (newStatus == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        order.setStatus(newStatus);
        return orderRepo.save(order);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        return switch (from) {
            case PLACED -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PACKED || to == OrderStatus.CANCELLED;
            case PACKED -> to == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

}
