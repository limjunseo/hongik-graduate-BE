package com.hongik.graduate.BE.auth.service;

public interface VerificationEmailSender {
    void send(String email, String code);
}
