package com.freshcart.dto;

import com.freshcart.entities.Product;
import org.springframework.data.domain.Page;
import java.util.List;

public record ProductPageResponse(List<Product> content, int page, int size, long totalElements, int totalPages, boolean last) {
    public static ProductPageResponse from(Page<Product> page) {
        return new ProductPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
