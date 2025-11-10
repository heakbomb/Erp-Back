package com.erp.erp_back.scheduler;

import java.time.LocalDateTime;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.enums.VerificationStatus;
import com.erp.erp_back.repository.auth.PhoneVerifyRequestRepository;
import com.erp.erp_back.service.auth.PhoneVerifyService;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;

@Component // @Service 대신 @Component 사용
@RequiredArgsConstructor
public class PhoneVerifyScheduler {

    private final PhoneVerifyService phoneVerifyService;
    private final PhoneVerifyRequestRepository phoneVerifyRepository;

    // --- Gmail IMAP 설정값 주입 ---
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private int port;
    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;

    /**
     * 스케줄러 1: 5초마다 이메일 수신함 확인
     */
    @Scheduled(fixedDelay = 5000)
    public void checkEmailInbox() {

        // ================================================
        // [하이브리드 로직 추가]
        // 1. 현재 DB에 PENDING 상태인 요청이 있는지 확인
        long pendingCount = phoneVerifyRepository.countByStatus(VerificationStatus.PENDING);

        // 2. PENDING 건이 0이면 (기다리는 사람이 없으면) Gmail 접속 안 함
        if (pendingCount == 0) {
            System.out.println("[" + java.time.LocalTime.now() + "] PENDING 요청 없음. (Skip)");
            return; // 작업 종료 (가장 효율적)
        }
        // ================================================

        System.out.println("[" + java.time.LocalTime.now() + "] 이메일 수신함 확인 중... (Pending: " + pendingCount + "건)");

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", port);
        props.put("mail.imap.ssl.enable", "true");

        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getDefaultInstance(props, null);
            store = session.getStore("imap");
            store.connect(host, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            if (messages.length == 0) return;
            
            System.out.println(">>> 새 메일 " + messages.length + "건 발견!");

            for (Message message : messages) {
                try {
                    String receivedCode = getTextFromMessage(message);
                    String cleanedBody = receivedCode.replaceAll("<[^>]*>", "").replaceAll("\\s+", "").trim();
                    
                    String finalAuthCode = (cleanedBody.length() >= 6) ? cleanedBody.substring(0, 6) : cleanedBody;

                    if (finalAuthCode.isBlank()) {
                         message.setFlag(Flags.Flag.SEEN, true);
                         continue;
                    }

                    // (수정) 파싱 로직은 Service의 헬퍼 메소드 사용
                    String receivedPhone = phoneVerifyService.parsePhoneNumberFromEmail(message);

                    if (receivedPhone == null) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        continue;
                    }

                    System.out.println("메일 처리 시도: Code=" + finalAuthCode + ", Phone=" + receivedPhone);
                    
                    // (수정) 검증 로직은 Service에 위임
                    phoneVerifyService.verifyEmailAuth(finalAuthCode, receivedPhone);
                    
                    message.setFlag(Flags.Flag.SEEN, true);
                    
                } catch (Exception e) {
                    System.out.println("메일 1건 처리 중 오류: " + e.getMessage());
                    if (message != null) message.setFlag(Flags.Flag.SEEN, true);
                }
            }
        } catch (AuthenticationFailedException e) {
            System.err.println("[치명적 오류] Gmail 인증 실패. 앱 비밀번호를 확인하세요.");
        } catch (Exception e) {
            System.out.println("이메일 게이트웨이 오류: " + e.getMessage());
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) inbox.close(true);
                if (store != null) store.close();
            } catch (MessagingException e) { e.printStackTrace(); }
        }
    }

    /**
     * 스케줄러 2: 10분마다 만료된 인증 요청 DB에서 삭제
     */
    @Scheduled(fixedDelay = 600000) // 10분 (10 * 60 * 1000)
    @Transactional
    public void cleanupExpiredRequests() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("[" + now + "] 만료된 인증 요청(phone_verify_requests) 청소 시작...");
        
        // Repository의 쿼리 호출
        phoneVerifyRepository.deleteAllByExpiresAtBefore(now);
        
        System.out.println("인증 요청 청소 완료.");
    }

    
    // --- 이메일 파싱 헬퍼 메소드들 ---
    
    private String getTextFromMessage(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        }
        return message.getContent().toString();
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return (String) bodyPart.getContent(); // Plain 텍스트 우선
            } else if (bodyPart.isMimeType("text/html")) {
                result.append((String) bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}
