package com.freshcart.dto;

import com.freshcart.entities.Product;
import java.math.BigDecimal;

public record CartItemResponse(Integer id, Product product, int quantity, BigDecimal lineTotal) {}
