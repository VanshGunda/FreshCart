package com.freshcart.controllers;

import com.freshcart.dto.UserResponse;
import com.freshcart.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> profile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> byId(@PathVariable Integer id, Authentication authentication) {
        if (!userService.getByUsername(authentication.getName()).getId().equals(id)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }
}
