package com.freshcart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentVerificationRequest {
    @NotBlank(message = "Razorpay order id is required")
    @Size(max = 100, message = "Razorpay order id is invalid")
    private String razorpayOrderId;
    @NotBlank(message = "Razorpay payment id is required")
    @Size(max = 100, message = "Razorpay payment id is invalid")
    private String razorpayPaymentId;
    @NotBlank(message = "Razorpay signature is required")
    @Size(max = 200, message = "Razorpay signature is invalid")
    private String razorpaySignature;
    @Valid
    @jakarta.validation.constraints.NotNull(message = "Shipping details are required")
    private CheckoutRequest shipping;

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    public CheckoutRequest getShipping() { return shipping; }
    public void setShipping(CheckoutRequest shipping) { this.shipping = shipping; }
}
