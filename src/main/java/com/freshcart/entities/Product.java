package com.freshcart.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_active", columnList = "active"),
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_brand", columnList = "brand")
    })
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price can have at most 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    @Column(nullable = false)
    private int stock;

    @JsonIgnore
    @Column(nullable = false)
    private int reservedStock;

    @Column(length = 500)
    private String imageUrl;

    @NotBlank(message = "Brand is required")
    @Column(nullable = false, length = 100)
    private String brand;

    @NotBlank(message = "Category is required")
    @Column(nullable = false, length = 100)
    private String category;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItem> cartItems) { this.cartItems = cartItems; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
}
