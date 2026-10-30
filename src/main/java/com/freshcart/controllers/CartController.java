package com.freshcart.controllers;

import com.freshcart.dto.CartResponse;
import com.freshcart.dto.QuantityRequest;
import com.freshcart.helpers.ApiResponse;
import com.freshcart.services.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping("/getC")
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    @GetMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> getCartItems(@PathVariable Integer cartId, Authentication authentication) {
        CartResponse cart = cartService.getCart(authentication.getName());
        if (!cart.id().equals(cartId)) throw new AccessDeniedException("Cart does not belong to the authenticated user");
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/add/{productId}")
    public ResponseEntity<CartResponse> add(@PathVariable Integer productId,
                                             @RequestParam(defaultValue = "1") int quantity,
                                             Authentication authentication) {
        return ResponseEntity.ok(cartService.addItem(authentication.getName(), productId, quantity));
    }

    @PutMapping("/increase/{cartItemId}")
    public ResponseEntity<CartResponse> increase(@PathVariable Integer cartItemId, Authentication authentication) {
        return ResponseEntity.ok(cartService.increase(authentication.getName(), cartItemId));
    }

    @PutMapping("/decrease/{cartItemId}")
    public ResponseEntity<CartResponse> decrease(@PathVariable Integer cartItemId, Authentication authentication) {
        return ResponseEntity.ok(cartService.decrease(authentication.getName(), cartItemId));
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<CartResponse> update(@PathVariable Integer id, @Valid @RequestBody QuantityRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(cartService.updateItem(authentication.getName(), id, request));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<CartResponse> remove(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(cartService.remove(authentication.getName(), id));
    }

    @PostMapping("/clear/{cartId}")
    public ResponseEntity<CartResponse> clear(@PathVariable Integer cartId, Authentication authentication) {
        return ResponseEntity.ok(cartService.clear(authentication.getName(), cartId));
    }
}
