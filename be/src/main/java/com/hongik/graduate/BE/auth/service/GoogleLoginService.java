package com.hongik.graduate.BE.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hongik.graduate.BE.auth.dto.TokenResponse;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.member.domain.AuthProvider;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleLoginService {
    private final MemberRepository members;
    private final AuthService auth;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate rest = new RestTemplate();

    public GoogleLoginService(MemberRepository members, AuthService auth,
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
            @Value("${GOOGLE_REDIRECT_URI:http://localhost:8080/login/oauth2/code/google}") String redirectUri) {
        this.members = members; this.auth = auth;
        this.clientId = clientId; this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String clientId() { return clientId; }
    public String redirectUri() { return redirectUri; }

    public record GoogleAccount(@JsonProperty("sub") String sub, String email,
                                @JsonProperty("email_verified") Boolean emailVerified) {}

    @Transactional
    public TokenResponse login(String code, String redirectUri) {
        if (code == null || code.isBlank() || redirectUri == null || !this.redirectUri.equals(redirectUri)
                || clientId.isBlank() || clientSecret.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            var params = new LinkedMultiValueMap<String, String>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseEntity<Map> token = rest.postForEntity(
                    "https://oauth2.googleapis.com/token", new HttpEntity<>(params,headers), Map.class);
            if (token.getBody() == null || !(token.getBody().get("access_token") instanceof String googleToken))
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            HttpHeaders bearer = new HttpHeaders();
            bearer.setBearerAuth(googleToken);
            var user = rest.exchange("https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET, new HttpEntity<Void>(bearer), GoogleAccount.class).getBody();
            if (user == null || user.sub() == null || user.sub().isBlank()
                    || user.email() == null || !Boolean.TRUE.equals(user.emailVerified())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            String email = auth.normalizeEmail(user.email());
            Member member = members.findByProviderAndProviderId(AuthProvider.GOOGLE,user.sub())
                    .orElseGet(() -> {
                        if (members.existsByEmail(email))
                            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
                        return members.saveAndFlush(Member.google(email,user.sub()));
                    });
            return auth.issueForAuthenticatedMember(member);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,exception);
        }
    }
}
