package com.freshcart.controllers;

import com.freshcart.dto.ProductRequest;
import com.freshcart.entities.Product;
import com.freshcart.services.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import com.freshcart.dto.ProductPageResponse;
import org.springframework.data.jpa.domain.Specification;

@RestController
@RequestMapping("/product")
@org.springframework.validation.annotation.Validated
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() { return ResponseEntity.ok(productService.getAllProducts()); }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam @NotBlank @Size(max = 100) String name) { return ResponseEntity.ok(productService.searchProducts(name)); }

    @GetMapping("/category")
    public ResponseEntity<List<Product>> byCategory(@RequestParam @NotBlank @Size(max = 100) String name) { return ResponseEntity.ok(productService.getByCategory(name)); }

    @GetMapping("/brand")
    public ResponseEntity<List<Product>> byBrand(@RequestParam @NotBlank @Size(max = 100) String name) { return ResponseEntity.ok(productService.getByBrand(name)); }

    @GetMapping("/price")
    public ResponseEntity<List<Product>> byPrice(@RequestParam @DecimalMin("0.00") BigDecimal min, @RequestParam @DecimalMin("0.00") BigDecimal max) { return ResponseEntity.ok(productService.getByPrice(min, max)); }

    @GetMapping("/catalog")
    public ResponseEntity<ProductPageResponse> catalog(
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) @Size(max = 100) String category,
            @RequestParam(required = false) @Size(max = 100) String brand,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal min,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        if (page < 0 || size < 1 || size > 50) throw new com.freshcart.exceptions.BadRequestException("Page must be >= 0 and size must be between 1 and 50");
        if (min != null && max != null && max.compareTo(min) < 0) throw new com.freshcart.exceptions.BadRequestException("Invalid price range");
        String[] sortParts = sort.split(",", 2);
        String field = switch (sortParts[0]) { case "name", "price", "id" -> sortParts[0]; default -> throw new com.freshcart.exceptions.BadRequestException("Unsupported sort field"); };
        Sort.Direction direction = sortParts.length == 2 && "asc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (search != null && !search.isBlank()) predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
            if (category != null && !category.isBlank()) predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            if (brand != null && !brand.isBlank()) predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            if (min != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            if (max != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), max));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<Product> result = productService.catalog(spec, PageRequest.of(page, size, Sort.by(direction, field)));
        return ResponseEntity.ok(ProductPageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Integer id) { return ResponseEntity.ok(productService.getProduct(id)); }
}
