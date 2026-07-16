package com.investlens.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
    private static final String SECRET = "test-secret-with-sufficient-entropy";
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void createsAndParsesSignedToken() {
        JwtTokenProvider provider = providerAt(NOW);
        User user = new User("  USER@Example.com ", "encoded-password");

        UserPrincipal principal = provider.parse(provider.createToken(user));

        assertThat(principal.id()).isEqualTo(user.getId());
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.role()).isEqualTo(user.getRole());
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = providerAt(NOW);
        String token = provider.createToken(new User("user@example.com", "encoded-password"));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertInvalidToken(() -> provider.parse(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        User user = new User("user@example.com", "encoded-password");
        String token = providerAt(NOW).createToken(user);
        JwtTokenProvider expiredProvider = providerAt(NOW.plusSeconds(61));

        assertInvalidToken(() -> expiredProvider.parse(token));
    }

    private static JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(new ObjectMapper(), SECRET, 60, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static void assertInvalidToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}
