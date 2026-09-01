package com.hongik.graduate.BE.auth.service;

import com.hongik.graduate.BE.auth.config.AuthProperties;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpVerificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    public SmtpVerificationEmailSender(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject("[pofit] 이메일 인증코드");
        message.setText("안녕하세요, pofit입니다.\n\n"
                + "회원가입을 완료하려면 아래 인증코드를 입력해주세요.\n\n"
                + "인증코드: " + code + "\n\n"
                + "인증코드는 10분 동안 유효합니다.\n"
                + "본인이 요청하지 않은 경우 이 메일을 무시해주세요.\n\n"
                + "감사합니다.\n"
                + "pofit 드림");
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("SMTP email delivery failed for host-configured mail sender", exception);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, exception);
        }
    }
}
