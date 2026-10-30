package com.freshcart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Integer id, List<CartItemResponse> items, BigDecimal totalAmount) {}
