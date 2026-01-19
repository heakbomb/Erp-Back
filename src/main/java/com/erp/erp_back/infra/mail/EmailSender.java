package com.erp.erp_back.infra.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("[요식업 ERP] 이메일 인증 코드");
        msg.setText("인증 코드 6자리: " + code + "\n\n3분 이내에 입력해주세요.");
        mailSender.send(msg);
    }
}