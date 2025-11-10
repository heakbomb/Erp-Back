package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.auth.PhoneVerifyRequestDto;
import com.erp.erp_back.dto.auth.PhoneVerifyResponseDto;
import com.erp.erp_back.dto.auth.PhoneVerifyStatusDto;
import com.erp.erp_back.entity.auth.PhoneVerifyRequest;
import com.erp.erp_back.entity.enums.VerificationStatus;
import com.erp.erp_back.repository.auth.PhoneVerifyRequestRepository;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneVerifyService {

    private final PhoneVerifyRequestRepository phoneVerifyRepository;

    /**
     * 1. 인증 요청 (API 호출 시)
     */
    @Transactional
    public PhoneVerifyResponseDto requestVerification(PhoneVerifyRequestDto requestDto) {
        
        String authCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String normalizedPhoneNumber = requestDto.getPhoneNumber().replaceAll("-", "");

        PhoneVerifyRequest request = PhoneVerifyRequest.builder()
                .phoneNumber(normalizedPhoneNumber)
                .authCode(authCode)
                .status(VerificationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(3)) // 3분 만료
                .build();

        phoneVerifyRepository.save(request);
        return new PhoneVerifyResponseDto(authCode);
    }

    /**
     * 2. 인증 상태 확인 (Front-end Polling)
     */
    @Transactional
    public PhoneVerifyStatusDto getVerificationStatus(String authCode) {
        
        Optional<PhoneVerifyRequest> optRequest = phoneVerifyRepository.findByAuthCode(authCode);

        if (optRequest.isEmpty()) {
            return new PhoneVerifyStatusDto("NOT_FOUND");
        }

        PhoneVerifyRequest request = optRequest.get();
        
        // PENDING 상태인데 만료시간이 지났다면, EXPIRED로 변경
        if (request.getStatus() == VerificationStatus.PENDING &&
            request.getExpiresAt().isBefore(LocalDateTime.now())) {
            
            request.expire(); 
            phoneVerifyRepository.save(request); // @Transactional이므로 자동 UPDATE
            return new PhoneVerifyStatusDto(VerificationStatus.EXPIRED);
        }

        // 현재 DB 상태 반환
        return new PhoneVerifyStatusDto(request.getStatus());
    }

    /**
     * 3. 이메일 검증 (스케줄러가 호출)
     */
    @Transactional
    public void verifyEmailAuth(String receivedCode, String receivedPhoneNumber) {
        Optional<PhoneVerifyRequest> optRequest = 
            phoneVerifyRepository.findByAuthCodeAndStatus(receivedCode, VerificationStatus.PENDING);

        if (optRequest.isEmpty()) {
            System.out.println("[인증 실패] " + receivedCode + "는 PENDING 상태가 아님");
            return;
        }
        
        PhoneVerifyRequest request = optRequest.get();

        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            System.out.println("[인증 실패] " + receivedCode + "는 만료됨");
            request.expire();
            phoneVerifyRepository.save(request);
            return;
        }

        // 전화번호 정규화 후 비교
        String dbPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");
        String emailPhone = receivedPhoneNumber.replaceAll("[^0-9]", "");
        
        if (dbPhone.equals(emailPhone)) {
            System.out.println("[인증 성공] " + receivedPhoneNumber + " (" + receivedCode + ")");
            request.verify();
            phoneVerifyRepository.save(request);
        } else {
            System.out.println("[인증 실패] " + receivedCode + "의 전화번호 불일치. DB: " 
                + dbPhone + ", 수신: " + emailPhone);
        }
    }

    /**
     * 4. 이메일 주소 파싱 (스케줄러가 호출)
     */
    public String parsePhoneNumberFromEmail(Message message) {
        try {
            Address from = message.getFrom()[0];
            String emailAddress = (from instanceof InternetAddress) ?
                ((InternetAddress) from).getAddress() : from.toString();
            
            String phoneNumber = emailAddress.split("@")[0];

            Pattern pattern = Pattern.compile("\\D"); // 숫자가 아닌 문자
            Matcher matcher = pattern.matcher(phoneNumber);
            String normalizedPhone = matcher.replaceAll("");
            
            if (normalizedPhone.startsWith("8210")) {
                return "0" + normalizedPhone.substring(2);
            }
            return normalizedPhone;

        } catch (Exception e) {
            System.out.println("[파싱 오류] " + e.getMessage());
            return null;
        }
    }
}