package com.freshcart.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"}),
        indexes = @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"))
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
