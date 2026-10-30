package com.freshcart.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpRequest {
    @NotBlank(message = "Username/email is required")
    @Email(message = "Username must be a valid email")
    @Size(max = 150, message = "Username cannot exceed 150 characters")
    private String username;
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
