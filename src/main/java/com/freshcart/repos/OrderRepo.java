package com.freshcart.repos;

import com.freshcart.entities.Order;
import com.freshcart.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Integer> {
    List<Order> findByUserOrderByIdDesc(User user);
    List<Order> findByUserIdOrderByIdDesc(Integer userId);
    Optional<Order> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
}
