package com.hongik.graduate.BE.auth.config;

import com.hongik.graduate.BE.auth.service.SmtpVerificationEmailSender;
import com.hongik.graduate.BE.auth.service.VerificationEmailSender;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MailConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MailConfiguration.class);

    @Bean
    VerificationEmailSender verificationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            AuthProperties properties
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            return new SmtpVerificationEmailSender(mailSender, properties);
        }

        log.error("JavaMailSender is not configured; email verification is unavailable");
        return (email, code) -> {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        };
    }
}
