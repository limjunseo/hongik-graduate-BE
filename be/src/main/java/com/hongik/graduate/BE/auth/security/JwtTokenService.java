package com.hongik.graduate.BE.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import com.hongik.graduate.BE.auth.config.AuthProperties;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.member.domain.Member;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(JwtEncoder encoder, JwtDecoder decoder, AuthProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    public TokenPair issue(Member member) {
        Instant now = Instant.now();
        String accessToken = encode(member, ACCESS, UUID.randomUUID().toString(), now,
                properties.accessTokenExpiration());
        String refreshToken = encode(member, REFRESH, UUID.randomUUID().toString(), now,
                properties.refreshTokenExpiration());
        return new TokenPair(accessToken, refreshToken, now.plusMillis(properties.accessTokenExpiration()),
                now.plusMillis(properties.refreshTokenExpiration()));
    }

    public Jwt decodeRefresh(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!REFRESH.equals(jwt.getClaimAsString("type"))) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            return jwt;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            String message = exception.getMessage();
            ErrorCode code = message != null && message.toLowerCase().contains("expired")
                    ? ErrorCode.REFRESH_TOKEN_EXPIRED
                    : ErrorCode.INVALID_REFRESH_TOKEN;
            throw new BusinessException(code, exception);
        }
    }

    public String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public String generateVerificationCode() {
        return String.format(Locale.ROOT, "%05d", secureRandom.nextInt(100000));
    }

    private String encode(Member member, String type, String jti, Instant now, long expiration) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(member.getId().toString())
                .claim("email", member.getEmail())
                .claim("type", type)
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expiration))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public record TokenPair(String accessToken, String refreshToken,
                            Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
    }
}
