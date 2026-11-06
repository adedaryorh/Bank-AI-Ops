package com.bankops.config;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void converterGrantsOnlyApplicationRoles() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("user")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "offline_access"))).build();
        var auth = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void decoderIsInitializedOnlyOnce() {
        var decoded = new java.util.ArrayList<String>();
        JwtDecoder delegate = token -> { decoded.add(token); return null; };
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        var decoder = new SecurityConfig.LazyJwtDecoder(() -> {
            calls.incrementAndGet();
            return delegate;
        });
        assertThat(calls.get()).isZero();
        decoder.decode("first");
        decoder.decode("second");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(decoded).containsExactly("first", "second");
    }
}
