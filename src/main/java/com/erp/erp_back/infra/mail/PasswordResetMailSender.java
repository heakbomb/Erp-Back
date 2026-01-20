package com.erp.erp_back.infra.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetMailSender {

    private final JavaMailSender mailSender;

    public void sendPasswordResetLink(String toEmail, String resetLink) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[요식업 ERP] 비밀번호 재설정 안내");

            String html =
                "<div style='font-family: Arial, sans-serif; line-height:1.6;'>"
              + "  <h2>비밀번호 재설정을 요청하셨습니다.</h2>"
              + "  <p>아래 버튼을 클릭하여 비밀번호를 재설정하세요.</p>"
              + "  <p style='margin: 24px 0;'>"
              + "    <a href='" + escapeAttr(resetLink) + "' "
              + "       style='display:inline-block; padding:12px 18px; background:#1f4b8f; color:#fff; text-decoration:none; border-radius:8px;'>"
              + "      비밀번호 재설정하기"
              + "    </a>"
              + "  </p>"
              + "  <p>또는 아래 링크를 복사해 주소창에 붙여넣으세요.</p>"
              + "  <p><a href='" + escapeAttr(resetLink) + "'>" + escapeHtml(resetLink) + "</a></p>"
              + "  <hr/>"
              + "  <p style='color:#666; font-size:12px;'>"
              + "    ※ 본 링크는 15분 후 만료됩니다.<br/>"
              + "    ※ 본인이 요청하지 않았다면 이 메일을 무시하세요."
              + "  </p>"
              + "</div>";

            helper.setText(html, true); // true = HTML
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // 여기서 RuntimeException으로 던져도 되고, 로그만 남겨도 됨(요구사항에 맞춰 선택)
            throw new IllegalStateException("비밀번호 재설정 메일 전송에 실패했습니다.");
        }
    }

    // 최소한의 방어 (속성/HTML 깨짐 방지)
    private static String escapeAttr(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}