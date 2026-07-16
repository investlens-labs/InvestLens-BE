package com.investlens.common.security;

import com.investlens.user.domain.User;
import com.investlens.user.domain.UserRole;
import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(UUID id, String email, UserRole role) implements Principal {
    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
