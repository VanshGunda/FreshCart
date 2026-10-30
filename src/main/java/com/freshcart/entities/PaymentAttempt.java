package com.freshcart.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_attempts",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "razorpay_order_id"),
            @UniqueConstraint(columnNames = "razorpay_payment_id")
        },
        indexes = {
            @Index(name = "idx_payment_attempt_user_status", columnList = "user_id,status"),
            @Index(name = "idx_payment_attempt_expires", columnList = "expires_at")
        })
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String razorpayOrderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 128)
    private String cartFingerprint;

    @Column(nullable = false, length = 10000)
    private String reservationSnapshot;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCartFingerprint() { return cartFingerprint; }
    public void setCartFingerprint(String cartFingerprint) { this.cartFingerprint = cartFingerprint; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getReservationSnapshot() { return reservationSnapshot; }
    public void setReservationSnapshot(String reservationSnapshot) { this.reservationSnapshot = reservationSnapshot; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
