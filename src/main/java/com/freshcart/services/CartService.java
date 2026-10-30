package com.freshcart.services;

import com.freshcart.dto.CartItemResponse;
import com.freshcart.dto.CartResponse;
import com.freshcart.dto.QuantityRequest;
import com.freshcart.entities.Cart;
import com.freshcart.entities.CartItem;
import com.freshcart.entities.Product;
import com.freshcart.entities.User;
import com.freshcart.exceptions.BadRequestException;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.CartItemRepo;
import com.freshcart.repos.CartRepo;
import com.freshcart.repos.ProductRepo;
import com.freshcart.repos.PaymentAttemptRepo;
import com.freshcart.entities.PaymentStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {
    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;
    private final UserService userService;
    private final PaymentAttemptRepo paymentAttemptRepo;

    public CartService(CartRepo cartRepo, CartItemRepo cartItemRepo, ProductRepo productRepo, UserService userService, PaymentAttemptRepo paymentAttemptRepo) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
        this.userService = userService;
        this.paymentAttemptRepo = paymentAttemptRepo;
    }

    @Transactional
    public Cart getOrCreateCart(String username) {
        User user = userService.getByUsername(username);
        return cartRepo.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            Cart saved = cartRepo.save(cart);
            user.setCart(saved);
            return saved;
        });
    }

    public CartResponse getCart(String username) {
        Cart cart = getOrCreateCart(username);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String username, Integer productId, int quantity) {
        assertNoActivePaymentReservation(username);
        if (quantity < 1 || quantity > 100) throw new BadRequestException("Quantity must be between 1 and 100");
        Cart cart = getOrCreateCart(username);
        Product product = productRepo.findActiveByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        CartItem item = cartItemRepo.findByCartAndProduct(cart, product).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(0);
            return newItem;
        });
        int newQuantity = item.getQuantity() + quantity;
        ensureStock(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepo.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String username, Integer itemId, QuantityRequest request) {
        assertNoActivePaymentReservation(username);
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepo.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        Product product = productRepo.findActiveByIdForUpdate(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ensureStock(product, request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepo.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse increase(String username, Integer itemId) {
        assertNoActivePaymentReservation(username);
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepo.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        Product product = productRepo.findActiveByIdForUpdate(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        int next = item.getQuantity() + 1;
        ensureStock(product, next);
        item.setQuantity(next);
        return toResponse(cartItemRepo.save(item).getCart());
    }

    @Transactional
    public CartResponse decrease(String username, Integer itemId) {
        assertNoActivePaymentReservation(username);
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepo.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (item.getQuantity() <= 1) cartItemRepo.delete(item);
        else item.setQuantity(item.getQuantity() - 1);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse remove(String username, Integer itemId) {
        assertNoActivePaymentReservation(username);
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepo.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepo.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clear(String username, Integer cartId) {
        assertNoActivePaymentReservation(username);
        Cart cart = getOrCreateCart(username);
        if (!cart.getId().equals(cartId)) throw new BadRequestException("Cart does not belong to the authenticated user");
        cartItemRepo.deleteByCartId(cartId);
        return toResponse(cart);
    }

    private void assertNoActivePaymentReservation(String username) {
        User user = userService.getByUsername(username);
        if (paymentAttemptRepo.existsByUserIdAndStatusAndExpiresAtAfter(user.getId(), PaymentStatus.PENDING, LocalDateTime.now())) {
            throw new BadRequestException("Cancel the active payment attempt before changing your cart");
        }
    }

    private void ensureStock(Product product, int quantity) {
        if (product.getStock() - product.getReservedStock() < quantity) throw new BadRequestException(
                product.getName() + " has only " + (product.getStock() - product.getReservedStock()) + " unit(s) in stock");
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepo.findByCartId(cart.getId()).stream()
                .map(i -> new CartItemResponse(i.getId(), i.getProduct(),
                        i.getQuantity(), i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
