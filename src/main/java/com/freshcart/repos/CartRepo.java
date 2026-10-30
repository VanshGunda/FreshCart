package com.freshcart.repos;

import com.freshcart.entities.Cart;
import com.freshcart.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CartRepo extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user = :user")
    Optional<Cart> findByUserForUpdate(@org.springframework.data.repository.query.Param("user") User user);
    Optional<Cart> findByUserId(Integer userId);
}
