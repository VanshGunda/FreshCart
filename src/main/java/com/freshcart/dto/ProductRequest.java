package com.freshcart.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price can have at most 2 decimal places")
    private BigDecimal price;
    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;
    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;
    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;
    @NotBlank(message = "Unit is required")
    @Size(max = 30, message = "Unit cannot exceed 30 characters")
    private String unit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
