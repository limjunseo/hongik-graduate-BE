package com.hongik.graduate.BE.auth.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.hongik.graduate.BE.auth.security.SecurityAccessDeniedHandler;
import com.hongik.graduate.BE.auth.security.SecurityAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityAuthenticationEntryPoint authenticationEntryPoint,
            SecurityAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/**",
                                "/h2-console/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties)).build();
    }

    private SecretKey secretKey(AuthProperties properties) {
        if (properties.jwtSecret() == null || properties.jwtSecret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(properties.jwtSecret());
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
            }
            return new SecretKeySpec(decoded, "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be a Base64 value", exception);
        }
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
