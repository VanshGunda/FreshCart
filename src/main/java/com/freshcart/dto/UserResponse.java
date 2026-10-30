package com.freshcart.dto;

import com.freshcart.entities.Role;

public record UserResponse(Integer id, String username, Role role, boolean verified) {}
