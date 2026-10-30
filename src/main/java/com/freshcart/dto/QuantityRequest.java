package com.freshcart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class QuantityRequest {
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100")
    private int quantity;

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
