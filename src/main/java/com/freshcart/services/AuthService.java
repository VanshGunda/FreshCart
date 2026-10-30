package com.freshcart.services;

import com.freshcart.dto.LoginRequest;
import com.freshcart.dto.LoginResponse;
import com.freshcart.dto.RegisterRequest;
import com.freshcart.dto.UserResponse;
import com.freshcart.entities.Cart;
import com.freshcart.entities.Role;
import com.freshcart.entities.User;
import com.freshcart.exceptions.BadRequestException;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.CartRepo;
import com.freshcart.repos.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserRepo userRepo;
    private final CartRepo cartRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepo userRepo, CartRepo cartRepo, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService, EmailService emailService) {
        this.userRepo = userRepo;
        this.cartRepo = cartRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String username = normalize(request.getUsername());
        User user = userRepo.findByUsername(username).orElseGet(User::new);
        if (user.getId() != null && user.isVerified() && user.isActive()) {
            throw new BadRequestException("An account already exists for this username");
        }
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setVerified(false);
        user.setActive(true);
        user.setDeletedAt(null);
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        User saved = userRepo.save(user);

        if (saved.getCart() == null) {
            Cart cart = new Cart();
            cart.setUser(saved);
            cartRepo.save(cart);
            saved.setCart(cart);
        }
        userRepo.save(saved);
        emailService.sendOtp(username, otp);
        return "Registration successful. Verify the OTP sent to your email.";
    }

    @Transactional
    public String verifyOtp(String username, String otp) {
        User user = userRepo.findByUsername(normalize(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isVerified()) return "Account is already verified";
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }
        if (user.getOtp() == null || !passwordEncoder.matches(otp, user.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepo.save(user);
        return "Account verified successfully";
    }

    public LoginResponse login(LoginRequest request) {
        String username = normalize(request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("Invalid username or password"));
            if (!user.isActive() || !user.isVerified()) {
                throw new BadRequestException("Invalid username or password");
            }
            String token = jwtService.generateToken((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());
            return new LoginResponse(token, toResponse(user));
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid username or password");
        }
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isVerified());
    }

    private String normalize(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }
}
