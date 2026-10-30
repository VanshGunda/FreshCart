package com.freshcart.repos;

import com.freshcart.entities.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.freshcart.entities.PaymentStatus;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PaymentAttemptRepo extends JpaRepository<PaymentAttempt, Integer> {
    Optional<PaymentAttempt> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PaymentAttempt p where p.user.id = :userId and p.cartFingerprint = :cartFingerprint order by p.id desc")
    List<PaymentAttempt> findRecentByUserIdAndCartFingerprintForUpdate(
            @org.springframework.data.repository.query.Param("userId") Integer userId,
            @org.springframework.data.repository.query.Param("cartFingerprint") String cartFingerprint);

    Optional<PaymentAttempt> findByRazorpayPaymentId(String razorpayPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PaymentAttempt p where p.razorpayOrderId = :razorpayOrderId")
    Optional<PaymentAttempt> findByRazorpayOrderIdForUpdate(@org.springframework.data.repository.query.Param("razorpayOrderId") String razorpayOrderId);

    Optional<PaymentAttempt> findFirstByUserIdAndCartFingerprintAndExpiresAtAfterOrderByIdDesc(
            Integer userId, String cartFingerprint, LocalDateTime now);

    boolean existsByUserIdAndStatusAndExpiresAtAfter(Integer userId, PaymentStatus status, LocalDateTime now);

    List<PaymentAttempt> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime now);
}
