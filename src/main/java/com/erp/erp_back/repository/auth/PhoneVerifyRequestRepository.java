package com.erp.erp_back.repository.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.auth.PhoneVerifyRequest;
import com.erp.erp_back.entity.enums.VerificationStatus;

public interface PhoneVerifyRequestRepository extends JpaRepository<PhoneVerifyRequest, Long> {

    // 1. 코드로 상태 조회 (Front-end Polling용)
    Optional<PhoneVerifyRequest> findByAuthCode(String authCode);

    // 2. 코드와 상태로 조회 (이메일 검증 시 사용)
    Optional<PhoneVerifyRequest> findByAuthCodeAndStatus(String authCode, VerificationStatus status);

    // 3. 만료된 모든 레코드를 삭제 (스케줄러 청소용)
    void deleteAllByExpiresAtBefore(LocalDateTime now);
    
    // [새로 추가] 특정 상태의 개수를 세는 쿼리 (하이브리드 스케줄러용)
    long countByStatus(VerificationStatus status);
}