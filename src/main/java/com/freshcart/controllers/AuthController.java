package com.freshcart.controllers;

import com.freshcart.dto.LoginRequest;
import com.freshcart.dto.LoginResponse;
import com.freshcart.dto.RegisterRequest;
import com.freshcart.entities.OtpRequest;
import com.freshcart.helpers.ApiResponse;
import com.freshcart.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/reg")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(request.getUsername(), request.getOtp())));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
