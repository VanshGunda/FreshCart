package com.freshcart.dto;

import com.freshcart.entities.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {
    @NotNull(message = "Order status is required")
    private OrderStatus status;

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
