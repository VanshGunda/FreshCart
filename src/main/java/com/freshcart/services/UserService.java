package com.freshcart.services;

import com.freshcart.dto.UserResponse;
import com.freshcart.entities.User;
import com.freshcart.exceptions.ResourceNotFoundException;
import com.freshcart.repos.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;
    public UserService(UserRepo userRepo) { this.userRepo = userRepo; }

    public User getByUsername(String username) {
        return userRepo.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getProfile(String username) { return AuthService.toResponse(getByUsername(username)); }
}
