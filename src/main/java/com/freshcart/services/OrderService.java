package com.freshcart.services;

import com.freshcart.dto.CheckoutRequest;
import com.freshcart.dto.PaymentVerificationRequest;
import com.freshcart.entities.*;
import com.freshcart.exceptions.BadRequestException;
import com.freshcart.exceptions.PaymentVerificationException;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.*;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final UserRepo userRepo;
    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final PaymentAttemptRepo paymentAttemptRepo;
    private final String razorpayKeyId;
    private final String razorpaySecret;
    private final long reservationMinutes;

    public OrderService(UserRepo userRepo, CartRepo cartRepo, CartItemRepo cartItemRepo,
                        ProductRepo productRepo, OrderRepo orderRepo, PaymentAttemptRepo paymentAttemptRepo,
                        @Value("${app.razorpay.key-id:}") String razorpayKeyId,
                        @Value("${app.razorpay.key-secret:}") String razorpaySecret,
                        @Value("${app.payment.reservation-minutes:15}") long reservationMinutes) {
        this.userRepo = userRepo;
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.paymentAttemptRepo = paymentAttemptRepo;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpaySecret = razorpaySecret;
        this.reservationMinutes = Math.max(5, reservationMinutes);
        this.autoRefundCapturedOrphanedPayment = autoRefundCapturedOrphanedPayment;
    }

    public List<Order> getMyOrders(String username) {
        User user = getUser(username);
        return orderRepo.findByUserOrderByIdDesc(user);
    }

    public List<Order> getOrdersByUser(Integer userId) {
        return orderRepo.findByUserIdOrderByIdDesc(userId);
    }

    public Integer getUserId(String username) { return getUser(username).getId(); }

    @Transactional
    public void deleteItemId(String username, Integer cartItemId) {
        User user = getUser(username);
        Cart cart = getCartForUpdate(user);
        CartItem item = cartItemRepo.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepo.delete(item);
    }

    @Transactional
    public Map<String, Object> createRazorpayOrder(String username) {
        ensureRazorpayConfigured();
        User user = getUser(username);
        Cart cart = getCartForUpdate(user);
        List<CartItem> items = cartItemRepo.findByCartId(cart.getId());
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");

        List<CartItem> orderedItems = items.stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();

        List<Product> lockedProducts = new ArrayList<>();
        for (CartItem item : orderedItems) {
            Product product = productRepo.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));
            lockedProducts.add(product);
        }

        Snapshot snapshot = buildSnapshot(orderedItems, lockedProducts);
        LocalDateTime now = LocalDateTime.now();

        // Recover/close attempts for the same cart safely before creating or reusing a payment order.
        List<PaymentAttempt> recentAttempts = paymentAttemptRepo
                .findRecentByUserIdAndCartFingerprintForUpdate(user.getId(), snapshot.fingerprint());

        if (!recentAttempts.isEmpty()) {
            PaymentAttempt latest = recentAttempts.get(0);

            if (latest.getStatus() == PaymentStatus.PENDING
                    && latest.getAmount().compareTo(snapshot.total()) == 0) {

                // Never reuse a Razorpay order that already has a captured payment.
                if (latest.getRazorpayPaymentId() != null) {
                    Optional<String> captured = findCapturedPaymentId(
                            latest.getRazorpayOrderId(), latest.getAmount());

                    if (captured.isPresent()) {
                        if (!captured.get().equals(latest.getRazorpayPaymentId())) {
                            latest.setRazorpayPaymentId(captured.get());
                            paymentAttemptRepo.save(latest);
                        }

                        if (autoRefundCapturedOrphanedPayment && latest.getExpiresAt().isBefore(now)) {
                            refundCapturedPayment(captured.get());
                            releaseReservation(latest);
                            latest.setStatus(PaymentStatus.REFUNDED);
                            paymentAttemptRepo.save(latest);
                            log.warn("Automatically refunded expired orphaned Razorpay payment in local recovery mode; paymentAttemptId={}", latest.getId());
                        } else {
                            throw new PaymentVerificationException(
                                    "A payment for this cart was already captured. Do not start another payment. Complete or reconcile the existing payment.");
                        }
                    } else {
                        // A stored payment id that is no longer captured is safe to close.
                        releaseReservation(latest);
                        latest.setStatus(PaymentStatus.FAILED);
                        paymentAttemptRepo.save(latest);
                    }
                } else if (!latest.getExpiresAt().isBefore(now)) {
                    return paymentResponse(
                            latest.getRazorpayOrderId(),
                            latest.getAmount(),
                            latest.getExpiresAt());
                } else {
                    releaseReservation(latest);
                    latest.setStatus(PaymentStatus.FAILED);
                    paymentAttemptRepo.save(latest);
                }
            }
        }

        reserve(snapshot, lockedProducts);

        try {
            int amountPaise = toPaise(snapshot.total());
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", "fc_" + user.getId() + "_" +
                    UUID.randomUUID().toString().replace("-", "").substring(0, 18));
            request.put("payment_capture", true);

            com.razorpay.Order razorpayOrder = client.orders.create(request);
            String externalId = responseValue(razorpayOrder.get("id"));
            if (externalId.isBlank()) {
                throw new BadRequestException("Razorpay did not return an order id");
            }

            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setRazorpayOrderId(externalId);
            attempt.setAmount(snapshot.total());
            attempt.setCartFingerprint(snapshot.fingerprint());
            attempt.setReservationSnapshot(snapshot.serialized());
            attempt.setExpiresAt(now.plusMinutes(reservationMinutes));
            attempt.setUser(user);
            attempt.setStatus(PaymentStatus.PENDING);
            paymentAttemptRepo.save(attempt);

            return paymentResponse(externalId, snapshot.total(), attempt.getExpiresAt());
        } catch (RazorpayException | ArithmeticException ex) {
            releaseReservation(snapshot);
            throw new BadRequestException("Unable to create Razorpay order");
        }
    }

    @Transactional
    public Order placeOrder(String username, PaymentVerificationRequest request) {
        ensureRazorpayConfigured();
        User user = getUser(username);

        Order existing = orderRepo.findByRazorpayPaymentId(request.getRazorpayPaymentId()).orElse(null);
        if (existing == null) {
            existing = orderRepo.findByRazorpayOrderId(request.getRazorpayOrderId()).orElse(null);
        }
        if (existing != null) {
            if (!existing.getUser().getId().equals(user.getId())) {
                throw new PaymentVerificationException("Payment does not belong to the authenticated user");
            }
            if (existing.getRazorpayPaymentId() != null
                    && !existing.getRazorpayPaymentId().equals(request.getRazorpayPaymentId())) {
                throw new PaymentVerificationException("Payment does not match the existing order");
            }
            if (existing.getRazorpayOrderId() != null
                    && !existing.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
                throw new PaymentVerificationException("Razorpay order does not match the existing order");
            }
            return existing;
        }

        PaymentAttempt attempt = paymentAttemptRepo.findByRazorpayOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentVerificationException("Razorpay order is not recognized"));
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new PaymentVerificationException("Payment order does not belong to the authenticated user");
        }
        if (attempt.getStatus() == PaymentStatus.PAID) {
            throw new PaymentVerificationException("This payment has already been completed");
        }
        if (attempt.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentVerificationException("This payment attempt is no longer active");
        }
        if (attempt.getRazorpayPaymentId() != null && !attempt.getRazorpayPaymentId().equals(request.getRazorpayPaymentId())) {
            throw new PaymentVerificationException("Payment does not match the captured payment for this checkout");
        }
        if (attempt.getExpiresAt().isBefore(LocalDateTime.now())) {
            // A captured payment may still be recoverable after the checkout reservation expired.
            if (attempt.getRazorpayPaymentId() != null) {
                String storedPaymentId = attempt.getRazorpayPaymentId();
                verifyCapturedPaymentState(
                        attempt.getRazorpayOrderId(),
                        storedPaymentId,
                        attempt.getAmount());
            } else {
                Optional<String> captured = findCapturedPaymentId(
                        attempt.getRazorpayOrderId(), attempt.getAmount());
                if (captured.isEmpty()) {
                    releaseReservation(attempt);
                    attempt.setStatus(PaymentStatus.FAILED);
                    paymentAttemptRepo.save(attempt);
                    throw new PaymentVerificationException("Payment session expired. Please start checkout again.");
                }
                attempt.setRazorpayPaymentId(captured.get());
                attempt.setExpiresAt(LocalDateTime.now().plusMinutes(reservationMinutes * 4));
                paymentAttemptRepo.save(attempt);
            }
        }

        Cart cart = getCartForUpdate(user);
        List<CartItem> cartItems = cartItemRepo.findByCartId(cart.getId());
        if (cartItems.isEmpty()) throw new BadRequestException("Cart is empty");
        String currentFingerprint = cartFingerprint(cartItems);
        if (!currentFingerprint.equals(attempt.getCartFingerprint())) {
            throw new PaymentVerificationException("Cart changed. Create a new payment order.");
        }

        if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            if (attempt.getRazorpayPaymentId() == null
                    || !attempt.getRazorpayPaymentId().equals(request.getRazorpayPaymentId())) {
                throw new PaymentVerificationException("Razorpay signature is required for a new payment");
            }
            verifyCapturedPaymentState(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    attempt.getAmount());
        } else {
            verifyRazorpayPayment(request, attempt.getAmount());
        }

        CheckoutRequest shipping = request.getShipping();
        if (shipping == null) throw new BadRequestException("Shipping details are required");

        Map<Integer, ReservedLine> reservedLines = parseSnapshot(attempt.getReservationSnapshot());
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(attempt.getAmount());
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPhone(shipping.getPhone().trim());
        order.setAddress(shipping.getAddress().trim());
        order.setCity(shipping.getCity().trim());
        order.setState(shipping.getState().trim());
        order.setPincode(shipping.getPincode().trim());
        order.setRazorpayOrderId(attempt.getRazorpayOrderId());
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());

        List<Integer> productIds = new ArrayList<>(reservedLines.keySet());
        Collections.sort(productIds);
        for (Integer productId : productIds) {
            ReservedLine line = reservedLines.get(productId);
            Product product = productRepo.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reserved product no longer exists"));
            if (product.getReservedStock() < line.quantity() || product.getStock() < line.quantity()) {
                throw new BadRequestException("Reserved inventory is no longer available for " + product.getName());
            }
            product.setReservedStock(product.getReservedStock() - line.quantity());
            product.setStock(product.getStock() - line.quantity());
            productRepo.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(line.quantity());
            orderItem.setPrice(line.price());
            order.getItems().add(orderItem);
        }

        Order saved = orderRepo.save(order);
        cartItemRepo.deleteByCartId(cart.getId());
        attempt.setRazorpayPaymentId(request.getRazorpayPaymentId());
        attempt.setStatus(PaymentStatus.PAID);
        paymentAttemptRepo.save(attempt);
        return saved;
    }

    @Transactional
    public void cancelPayment(String username, String razorpayOrderId) {
        User user = getUser(username);
        PaymentAttempt attempt = paymentAttemptRepo.findByRazorpayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found"));
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new PaymentVerificationException("Payment attempt does not belong to the authenticated user");
        }
        if (attempt.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Completed payments cannot be cancelled here");
        }
        if (attempt.getRazorpayPaymentId() != null || hasCapturedRazorpayPayment(attempt.getRazorpayOrderId())) {
            throw new BadRequestException("Payment has already been captured. Complete order verification instead of cancelling the payment session.");
        }
        releaseReservation(attempt);
        attempt.setStatus(PaymentStatus.CANCELLED);
        paymentAttemptRepo.save(attempt);
    }

    @Scheduled(fixedDelayString = "${app.payment.cleanup-delay-ms:60000}")
    @Transactional
    public void expirePendingPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentAttempt> expired = paymentAttemptRepo.findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, now);
        for (PaymentAttempt attempt : expired) {
            try {
                Optional<String> capturedPaymentId = findCapturedPaymentId(attempt.getRazorpayOrderId(), attempt.getAmount());
                if (capturedPaymentId.isPresent()) {
                    attempt.setRazorpayPaymentId(capturedPaymentId.get());
                    attempt.setExpiresAt(now.plusMinutes(reservationMinutes * 4));
                    paymentAttemptRepo.save(attempt);
                    log.warn("Captured Razorpay payment awaiting FreshCart finalization; paymentAttemptId={}", attempt.getId());
                    continue;
                }
                releaseReservation(attempt);
                attempt.setStatus(PaymentStatus.FAILED);
                paymentAttemptRepo.save(attempt);
            } catch (RuntimeException ex) {
                log.error("Failed to reconcile expired payment reservation id={}", attempt.getId(), ex);
            }
        }
    }

    private void refundCapturedPayment(String razorpayPaymentId) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            com.razorpay.Payment payment = client.payments.fetch(razorpayPaymentId);
            int amount = Integer.parseInt(responseValue(payment.get("amount")));
            int amountRefunded = Integer.parseInt(responseValue(payment.get("amount_refunded")));

            if (amountRefunded < amount) {
                client.payments.refund(razorpayPaymentId);
            }
        } catch (RazorpayException | NumberFormatException | ArithmeticException ex) {
            throw new PaymentVerificationException("Unable to safely refund the previous local test payment", ex);
        }
    }

    private boolean hasCapturedRazorpayPayment(String razorpayOrderId) {
        return findCapturedPaymentId(razorpayOrderId, null).isPresent();
    }

    private Optional<String> findCapturedPaymentId(String razorpayOrderId, BigDecimal expectedAmount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            List<com.razorpay.Payment> payments = client.orders.fetchPayments(razorpayOrderId);
            int expectedPaise = expectedAmount == null ? -1 : toPaise(expectedAmount);
            return payments.stream()
                    .filter(p -> "captured".equalsIgnoreCase(responseValue(p.get("status"))))
                    .filter(p -> expectedAmount == null || Integer.parseInt(responseValue(p.get("amount"))) == expectedPaise)
                    .map(p -> responseValue(p.get("id")))
                    .findFirst();
        } catch (RazorpayException | NumberFormatException | ArithmeticException ex) {
            throw new BadRequestException("Unable to reconcile Razorpay payment");
        }
    }

    /**
     * Razorpay's SDK exposes response fields through a generic get(...) method.
     * Passing that generic result directly to String.valueOf(...) can make Java
     * select the char[] overload and cause a runtime ClassCastException when the
     * actual SDK value is a String. Normalize all SDK response values through an
     * Object-typed method parameter instead.
     */
    private static String responseValue(Object value) {
        return Objects.toString(value, "");
    }

    private void verifyCapturedPaymentState(String razorpayOrderId,
                                            String razorpayPaymentId,
                                            BigDecimal expectedAmount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            com.razorpay.Payment payment = client.payments.fetch(razorpayPaymentId);
            String paymentOrderId = responseValue(payment.get("order_id"));
            String status = responseValue(payment.get("status"));
            int amountPaise = Integer.parseInt(responseValue(payment.get("amount")));

            if (!razorpayOrderId.equals(paymentOrderId)) {
                throw new PaymentVerificationException("Payment/order mismatch");
            }
            if (amountPaise != toPaise(expectedAmount)) {
                throw new PaymentVerificationException("Payment amount mismatch");
            }
            if (!"captured".equalsIgnoreCase(status)) {
                throw new PaymentVerificationException("Payment has not been captured");
            }
        } catch (RazorpayException | NumberFormatException | ArithmeticException ex) {
            throw new PaymentVerificationException("Unable to verify captured Razorpay payment", ex);
        }
    }

    private void verifyRazorpayPayment(PaymentVerificationRequest request, BigDecimal expectedAmount) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            if (!Utils.verifyPaymentSignature(attributes, razorpaySecret)) {
                throw new PaymentVerificationException("Invalid Razorpay payment signature");
            }

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            com.razorpay.Payment payment = client.payments.fetch(request.getRazorpayPaymentId());
            String orderId = responseValue(payment.get("order_id"));
            String status = responseValue(payment.get("status"));
            int amountPaise = Integer.parseInt(responseValue(payment.get("amount")));
            int expectedPaise = toPaise(expectedAmount);
            if (!request.getRazorpayOrderId().equals(orderId)) throw new PaymentVerificationException("Payment/order mismatch");
            if (amountPaise != expectedPaise) throw new PaymentVerificationException("Payment amount mismatch");
            if (!"captured".equalsIgnoreCase(status)) throw new PaymentVerificationException("Payment has not been captured");
        } catch (RazorpayException | NumberFormatException | ArithmeticException ex) {
            throw new PaymentVerificationException("Unable to verify Razorpay payment", ex);
        }
    }

    private Snapshot buildSnapshot(List<CartItem> items, List<Product> products) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder fingerprintSource = new StringBuilder();
        StringBuilder snapshot = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            Product product = products.get(i);
            if (!product.isActive()) throw new ResourceNotFoundException("Product not found in cart");
            if (item.getQuantity() < 1 || item.getQuantity() > 100) throw new BadRequestException("Invalid cart quantity");
            int available = product.getStock() - product.getReservedStock();
            if (available < item.getQuantity()) throw new BadRequestException(product.getName() + " has insufficient stock");
            BigDecimal price = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            fingerprintSource.append(product.getId()).append(':').append(item.getQuantity()).append('|');
            snapshot.append(product.getId()).append(':').append(item.getQuantity()).append(':').append(price.toPlainString()).append('|');
        }
        return new Snapshot(total.setScale(2, RoundingMode.HALF_UP), sha256(fingerprintSource.toString()), snapshot.toString());
    }

    private void reserve(Snapshot snapshot, List<Product> products) {
        Map<Integer, ReservedLine> lines = parseSnapshot(snapshot.serialized());
        for (Product product : products) {
            ReservedLine line = lines.get(product.getId());
            if (line == null) continue;
            int available = product.getStock() - product.getReservedStock();
            if (available < line.quantity()) throw new BadRequestException(product.getName() + " has insufficient stock");
            product.setReservedStock(product.getReservedStock() + line.quantity());
            productRepo.save(product);
        }
    }

    private void releaseReservation(PaymentAttempt attempt) {
        releaseReservation(new Snapshot(attempt.getAmount(), attempt.getCartFingerprint(), attempt.getReservationSnapshot()));
    }

    private void releaseReservation(Snapshot snapshot) {
        Map<Integer, ReservedLine> lines = parseSnapshot(snapshot.serialized());
        List<Integer> ids = new ArrayList<>(lines.keySet());
        Collections.sort(ids);
        for (Integer id : ids) {
            Product product = productRepo.findByIdForUpdate(id).orElse(null);
            if (product == null) continue;
            ReservedLine line = lines.get(id);
            product.setReservedStock(Math.max(0, product.getReservedStock() - line.quantity()));
            productRepo.save(product);
        }
    }

    private Map<Integer, ReservedLine> parseSnapshot(String snapshot) {
        Map<Integer, ReservedLine> result = new HashMap<>();
        if (snapshot == null || snapshot.isBlank()) return result;
        for (String token : snapshot.split("\\|")) {
            if (token.isBlank()) continue;
            String[] parts = token.split(":", 3);
            if (parts.length != 3) throw new IllegalStateException("Invalid payment reservation snapshot");
            result.put(Integer.valueOf(parts[0]), new ReservedLine(Integer.parseInt(parts[1]), new BigDecimal(parts[2]).setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private String cartFingerprint(List<CartItem> items) {
        String canonical = items.stream().sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .map(i -> i.getProduct().getId() + ":" + i.getQuantity())
                .reduce("", (a, b) -> a + b + "|");
        return sha256(canonical);
    }

    private Map<String, Object> paymentResponse(String id, BigDecimal amount, LocalDateTime expiresAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("amount", toPaise(amount));
        response.put("currency", "INR");
        response.put("key", razorpayKeyId);
        response.put("expiresAt", expiresAt);
        return response;
    }

    private int toPaise(BigDecimal amount) {
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Payment amount is outside the supported range");
        }
    }

    private String sha256(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private User getUser(String username) {
        return userRepo.findByUsername(username.trim().toLowerCase())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Cart getCartForUpdate(User user) {
        return cartRepo.findByUserForUpdate(user).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId.isBlank() || razorpaySecret.isBlank()) {
            throw new BadRequestException("Razorpay is not configured");
        }
    }

    private record Snapshot(BigDecimal total, String fingerprint, String serialized) {}
    private record ReservedLine(int quantity, BigDecimal price) {}
}
