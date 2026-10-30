package com.freshcart.controllers;

import com.freshcart.dto.ProductRequest;
import com.freshcart.dto.StatusUpdateRequest;
import com.freshcart.entities.Order;
import com.freshcart.entities.Product;
import com.freshcart.entities.User;
import com.freshcart.helpers.ApiResponse;
import com.freshcart.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() { return ResponseEntity.ok(adminService.getDashboard()); }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> products() { return ResponseEntity.ok(adminService.getAllProducts()); }

    @PostMapping("/products")
    public ResponseEntity<Product> addProduct(@Valid @RequestBody ProductRequest request) { return ResponseEntity.ok(adminService.addProduct(request)); }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) { return ResponseEntity.ok(adminService.updateProduct(id, request)); }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Integer id) { adminService.deleteProduct(id); return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully")); }

    @GetMapping("/users")
    public ResponseEntity<List<User>> users() { return ResponseEntity.ok(adminService.getAllUsers()); }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Integer id) { adminService.deleteUser(id); return ResponseEntity.ok(ApiResponse.ok("User deleted successfully")); }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> orders() { return ResponseEntity.ok(adminService.getAllOrders()); }

    @PostMapping("/orders/{id}/cancel-refund")
    public ResponseEntity<Order> cancelAndRefund(@PathVariable Integer id) { return ResponseEntity.ok(adminService.cancelAndRefund(id)); }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateOrderStatus(id, request.getStatus()));
    }
}
