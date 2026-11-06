package com.bankops.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.bankops.identity.UserProvisioningFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless OAuth2 resource server. Roles arrive as {@code realm_access.roles}
 * and are mapped to {@code ROLE_*} authorities; the audience is validated by the
 * {@code spring.security.oauth2.resourceserver.jwt.audiences} property.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               BankOpsProperties properties,
                                               Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                               AuthenticationEntryPoint problemAuthenticationEntryPoint,
                                               AccessDeniedHandler problemAccessDeniedHandler,
                                               UserProvisioningFilter userProvisioningFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "OPERATIONS_OFFICER",
                    "COMPLIANCE_ANALYST", "AML_ANALYST", "SUPERVISOR", "AUDITOR")
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(problemAuthenticationEntryPoint)
                .accessDeniedHandler(problemAccessDeniedHandler))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(problemAuthenticationEntryPoint)
                .accessDeniedHandler(problemAccessDeniedHandler))
            .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<UserProvisioningFilter> provisioningRegistration(
            UserProvisioningFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Decoder built lazily: the OIDC discovery document is fetched on the first
     * token validation, not at startup. That keeps tests and offline starts fast and
     * lets the API boot before Keycloak is ready.
     */
    @Bean
    JwtDecoder jwtDecoder(BankOpsProperties properties) {
        String issuer = properties.security().issuerUri();
        String audience = properties.security().audience();
        return new LazyJwtDecoder(() -> {
            String jwkSetUri = properties.security().jwkSetUri();
            NimbusJwtDecoder decoder = jwkSetUri == null || jwkSetUri.isBlank()
                ? NimbusJwtDecoder.withIssuerLocation(issuer).build()
                : NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> audienceValidator = token ->
                token.getAudience().contains(audience)
                    ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                    : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Token audience does not contain " + audience, null));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
            return decoder;
        });
    }

    /** Resolves the real decoder on first use and caches it. */
    static final class LazyJwtDecoder implements JwtDecoder {
        private final java.util.function.Supplier<JwtDecoder> supplier;
        private volatile JwtDecoder delegate;

        LazyJwtDecoder(java.util.function.Supplier<JwtDecoder> supplier) {
            this.supplier = supplier;
        }

        private JwtDecoder delegate() {
            JwtDecoder local = delegate;
            if (local == null) {
                synchronized (this) {
                    local = delegate;
                    if (local == null) {
                        local = supplier.get();
                        delegate = local;
                    }
                }
            }
            return local;
        }

        @Override
        public Jwt decode(String token) throws org.springframework.security.oauth2.jwt.JwtException {
            return delegate().decode(token);
        }
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            List<String> roles = com.bankops.common.JwtClaims.realmRoles(jwt);
            Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    AuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return (request, response, ex) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
            "Authentication required", "A valid bearer token for this API is required.");
    }

    @Bean
    AccessDeniedHandler problemAccessDeniedHandler() {
        return (request, response, ex) -> writeProblem(response, HttpStatus.FORBIDDEN,
            "Access denied", "Your role is not allowed to perform this operation.");
    }

    private static void writeProblem(jakarta.servlet.http.HttpServletResponse response,
                                     HttpStatus status, String title, String detail) {
        try {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
            problem.setTitle(title);
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}"""
                .formatted(title, status.value(), detail));
        } catch (java.io.IOException ignored) {
            // The container already aborted the response; nothing useful to do.
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(BankOpsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = properties.cors() != null && properties.cors().allowedOrigins() != null
            ? properties.cors().allowedOrigins()
            : List.of("http://localhost:4200");
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
