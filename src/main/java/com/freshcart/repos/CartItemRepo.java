package com.freshcart.repos;

import com.freshcart.entities.Cart;
import com.freshcart.entities.CartItem;
import com.freshcart.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepo extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByIdAndCart(Integer id, Cart cart);
    void deleteByCartId(Integer cartId);
}
