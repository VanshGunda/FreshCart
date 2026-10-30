package com.freshcart.services;

import com.freshcart.dto.ProductRequest;
import com.freshcart.entities.Product;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.ProductRepo;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepo productRepo;

    public ProductService(ProductRepo productRepo) { this.productRepo = productRepo; }

    public Page<Product> catalog(Specification<Product> specification, Pageable pageable) { return productRepo.findAll(specification, pageable); }

    public List<Product> getAllProducts() { return productRepo.findByActiveTrueOrderByIdDesc(); }
    public List<Product> getAllProductsForAdmin() { return productRepo.findAll(Sort.by(Sort.Direction.DESC, "id")); }
    public Product getProduct(Integer id) {
        return productRepo.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
    public List<Product> searchProducts(String name) { return productRepo.findByActiveTrueAndNameContainingIgnoreCase(name.trim()); }
    public List<Product> getByCategory(String category) { return productRepo.findByActiveTrueAndCategoryIgnoreCase(category.trim()); }
    public List<Product> getByBrand(String brand) { return productRepo.findByActiveTrueAndBrandIgnoreCase(brand.trim()); }
    public List<Product> getByPrice(BigDecimal min, BigDecimal max) {
        if (min == null || max == null || min.signum() < 0 || max.compareTo(min) < 0) {
            throw new com.freshcart.exceptions.BadRequestException("Invalid price range");
        }
        return productRepo.findByActiveTrueAndPriceBetween(min, max);
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product p = new Product();
        map(p, request);
        return productRepo.save(p);
    }

    @Transactional
    public Product update(Integer id, ProductRequest request) {
        Product p = productRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        map(p, request);
        if (p.getStock() < p.getReservedStock()) {
            throw new com.freshcart.exceptions.BadRequestException("Stock cannot be lower than reserved inventory");
        }
        p.setActive(true);
        return productRepo.save(p);
    }

    @Transactional
    public void delete(Integer id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        p.setActive(false);
        productRepo.save(p);
    }

    private void map(Product p, ProductRequest r) {
        p.setName(r.getName().trim());
        p.setDescription(r.getDescription().trim());
        p.setPrice(r.getPrice().setScale(2));
        p.setStock(r.getStock());
        if (r.getStock() < 0) throw new com.freshcart.exceptions.BadRequestException("Stock cannot be negative");
        p.setImageUrl(r.getImageUrl());
        p.setBrand(r.getBrand().trim());
        p.setCategory(r.getCategory().trim());
        p.setUnit(r.getUnit().trim());
    }
}
