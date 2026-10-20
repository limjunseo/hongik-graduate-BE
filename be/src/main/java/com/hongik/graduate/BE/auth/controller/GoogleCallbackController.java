package com.hongik.graduate.BE.auth.controller;

import com.hongik.graduate.BE.auth.dto.TokenResponse;
import com.hongik.graduate.BE.auth.service.GoogleLoginService;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@Profile("local")
public class GoogleCallbackController {
    private static final String STATE_COOKIE = "pofit_oauth_state";
    private final GoogleLoginService google;
    private final SecureRandom random = new SecureRandom();

    public GoogleCallbackController(GoogleLoginService google) {
        this.google = google;
    }

    @GetMapping("/oauth2/authorization/google")
    public ResponseEntity<Void> authorize(HttpServletResponse response) {
        if (google.clientId().isBlank()) return ResponseEntity.status(503).build();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        response.addHeader("Set-Cookie", STATE_COOKIE + "=" + state
                + "; Path=/login/oauth2/code/google; HttpOnly; SameSite=Lax; Max-Age=300");
        String url = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", google.clientId())
                .queryParam("redirect_uri", google.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build().encode().toUriString();
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<?> callback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @CookieValue(name = STATE_COOKIE, required = false) String savedState,
            HttpServletResponse response) {
        response.addHeader("Set-Cookie", STATE_COOKIE + "=; Path=/login/oauth2/code/google; HttpOnly; SameSite=Lax; Max-Age=0");
        if (code == null || code.isBlank() || state == null || savedState == null
                || !java.security.MessageDigest.isEqual(
                    state.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    savedState.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body("Invalid OAuth state or code");
        }
        TokenResponse tokens = google.login(code, google.redirectUri());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }
}
