package com.freshcart.repos;

import com.freshcart.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProductRepo extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findByActiveTrue();
    long countByActiveTrue();
    List<Product> findByActiveTrueOrderByIdDesc();
    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);
    List<Product> findByActiveTrueAndCategoryIgnoreCase(String category);
    List<Product> findByActiveTrueAndBrandIgnoreCase(String brand);
    List<Product> findByActiveTrueAndPriceBetween(BigDecimal min, BigDecimal max);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id and p.active = true")
    Optional<Product> findActiveByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
