package com.freshcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {
    @NotBlank(message = "Username/email is required")
    @jakarta.validation.constraints.Email(message = "Username must be a valid email")
    @jakarta.validation.constraints.Size(max = 150, message = "Username cannot exceed 150 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,72}$",
        message = "Password must contain at least 8 characters, one uppercase, one lowercase, one digit and one special character"
    )
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
