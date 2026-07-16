package com.investlens.user.dto;

import com.investlens.user.domain.User;
import com.investlens.user.domain.UserRole;
import java.util.UUID;

public record UserResponse(UUID id, String email, UserRole role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
