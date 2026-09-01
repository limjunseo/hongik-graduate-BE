package com.hongik.graduate.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import com.hongik.graduate.BE.auth.service.VerificationEmailSender;
import com.hongik.graduate.BE.member.repository.EmailVerificationRepository;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.BE.member.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(AuthIntegrationTest.TestMailConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CapturingEmailSender emailSender;

    private static final AtomicReference<String> sentCode = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        verificationRepository.deleteAll();
        memberRepository.deleteAll();
        emailSender.reset();
    }

    @Test
    void signupRequiresVerifiedEmailAndCanLoginAfterVerification() throws Exception {
        sendCode();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_NOT_VERIFIED"));

        verifyCode();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void resendIsRateLimitedAndInvalidCodeHasStableErrorCode() throws Exception {
        sendCode();

        mockMvc.perform(post("/api/auth/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("VERIFICATION_REQUEST_TOO_FREQUENT"));

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"WRONG123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VERIFICATION_CODE_INVALID"));
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesTheRotatedToken() throws Exception {
        completeSignup();
        String refreshToken = loginAndGetRefreshToken();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();
        String rotatedRefreshToken = JsonPath.read(
                refreshResult.getResponse().getContentAsString(), "$.data.refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void invalidCredentialsAreRejectedWithoutRevealingWhichValueFailed() throws Exception {
        completeSignup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void swaggerDocumentationIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    private void completeSignup() throws Exception {
        sendCode();
        verifyCode();
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");
    }

    private void sendCode() throws Exception {
        mockMvc.perform(post("/api/auth/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"USER@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void verifyCode() throws Exception {
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"email\":\"user@example.com\",\"code\":\""
                                + sentCode.get() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @TestConfiguration
    static class TestMailConfiguration {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    static class CapturingEmailSender implements VerificationEmailSender {
        @Override
        public void send(String email, String code) {
            sentCode.set(code);
        }

        void reset() {
            sentCode.set(null);
        }
    }
}
