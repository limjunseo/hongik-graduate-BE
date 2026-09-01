package com.hongik.graduate.BE.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        long emailVerificationExpiration,
        long emailVerificationResendInterval,
        int emailVerificationMaxAttempts,
        String mailFrom
) {
}
