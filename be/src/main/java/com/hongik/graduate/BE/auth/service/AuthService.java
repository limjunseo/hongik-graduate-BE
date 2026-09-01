package com.hongik.graduate.BE.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import com.hongik.graduate.BE.auth.config.AuthProperties;
import com.hongik.graduate.BE.auth.dto.LoginRequest;
import com.hongik.graduate.BE.auth.dto.RefreshTokenRequest;
import com.hongik.graduate.BE.auth.dto.SignupRequest;
import com.hongik.graduate.BE.auth.dto.TokenResponse;
import com.hongik.graduate.BE.auth.security.JwtTokenService;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.member.domain.EmailVerification;
import com.hongik.graduate.BE.member.domain.AuthProvider;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.domain.RefreshToken;
import com.hongik.graduate.BE.member.repository.EmailVerificationRepository;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.BE.member.repository.RefreshTokenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository verificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final VerificationEmailSender emailSender;
    private final AuthProperties properties;

    public AuthService(MemberRepository memberRepository,
                       EmailVerificationRepository verificationRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       VerificationEmailSender emailSender,
                       AuthProperties properties) {
        this.memberRepository = memberRepository;
        this.verificationRepository = verificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Transactional
    public void sendVerification(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Instant now = Instant.now();
        EmailVerification verification = verificationRepository.findByEmail(email).orElse(null);
        if (verification != null) {
            if (verification.getVerifiedAt() != null
                    && verification.getExpiresAt().isAfter(now)) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            }
            if (now.isBefore(verification.getSentAt().plusMillis(
                    properties.emailVerificationResendInterval()))) {
                throw new BusinessException(ErrorCode.VERIFICATION_REQUEST_TOO_FREQUENT);
            }
        }

        String code = jwtTokenService.generateVerificationCode();
        String codeHash = hash(code);
        Instant expiresAt = now.plusMillis(properties.emailVerificationExpiration());
        if (verification == null) {
            verification = new EmailVerification(email, codeHash, now, expiresAt);
        } else {
            verification.replaceCode(codeHash, now, expiresAt);
        }
        verificationRepository.save(verification);
        emailSender.send(email, code);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyEmail(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = verificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
        Instant now = Instant.now();
        if (!verification.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getAttempts() >= properties.emailVerificationMaxAttempts()
                || !MessageDigest.isEqual(
                        verification.getCodeHash().getBytes(StandardCharsets.UTF_8),
                        hash(code).getBytes(StandardCharsets.UTF_8))) {
            verification.increaseAttempts();
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        verification.verify(now);
    }

    @Transactional
    public Member signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        EmailVerification verification = verificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        Instant now = Instant.now();
        if (verification.getVerifiedAt() == null
                || !verification.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        try {
            Member member = memberRepository.saveAndFlush(
                    new Member(email, passwordEncoder.encode(request.password())));
            verificationRepository.deleteByEmail(email);
            return member;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, exception);
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (member.getProvider() != AuthProvider.LOCAL
                || member.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(member);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        Jwt jwt = jwtTokenService.decodeRefresh(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(
                        jwtTokenService.hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!stored.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        stored.revoke();
        return issueTokens(stored.getMember());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(jwtTokenService.hash(request.refreshToken()))
                .ifPresent(RefreshToken::revoke);
    }

    private TokenResponse issueTokens(Member member) {
        JwtTokenService.TokenPair tokens = jwtTokenService.issue(member);
        Jwt refresh = jwtTokenService.decodeRefresh(tokens.refreshToken());
        refreshTokenRepository.save(new RefreshToken(
                member,
                jwtTokenService.hash(tokens.refreshToken()),
                refresh.getId(),
                tokens.refreshTokenExpiresAt()
        ));
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer",
                tokens.accessTokenExpiresAt(), tokens.refreshTokenExpiresAt());
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
