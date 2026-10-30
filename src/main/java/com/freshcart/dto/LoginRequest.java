package com.freshcart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Username/email is required")
    @Email(message = "Username must be a valid email")
    @Size(max = 150, message = "Username cannot exceed 150 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password cannot exceed 72 characters")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
