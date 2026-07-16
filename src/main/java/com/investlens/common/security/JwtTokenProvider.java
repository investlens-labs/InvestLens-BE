package com.investlens.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.user.domain.User;
import com.investlens.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HEADER = URL_ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(ObjectMapper objectMapper,
                            @Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this(objectMapper, secret, expirationSeconds, Clock.systemUTC());
    }

    JwtTokenProvider(ObjectMapper objectMapper, String secret, long expirationSeconds, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    public String createToken(User user) {
        Instant now = clock.instant();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());
        try {
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String content = HEADER + "." + payload;
            return content + "." + URL_ENCODER.encodeToString(sign(content));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create authentication token", e);
        }
    }

    public UserPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                throw invalidToken();
            }
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content), URL_DECODER.decode(parts[2]))) {
                throw invalidToken();
            }
            Map<String, Object> claims = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            long expiresAt = requireLong(claims.get("exp"));
            if (expiresAt <= clock.instant().getEpochSecond()) {
                throw invalidToken();
            }
            UUID id = UUID.fromString(requireString(claims.get("sub")));
            String email = requireString(claims.get("email"));
            UserRole role = UserRole.valueOf(requireString(claims.get("role")));
            return new UserPrincipal(id, email, role);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw invalidToken();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
    }

    private static String requireString(Object value) {
        if (!(value instanceof String string) || string.isBlank()) throw invalidToken();
        return string;
    }

    private static long requireLong(Object value) {
        if (!(value instanceof Number number)) throw invalidToken();
        return number.longValue();
    }

    private static BusinessException invalidToken() {
        return new BusinessException(ErrorCode.INVALID_TOKEN);
    }
}
