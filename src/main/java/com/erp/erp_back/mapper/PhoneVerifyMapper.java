package com.erp.erp_back.mapper;

import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.auth.PhoneVerifyResponseDto;
import com.erp.erp_back.dto.auth.PhoneVerifyStatusDto;
import com.erp.erp_back.entity.auth.PhoneVerifyRequest;
import com.erp.erp_back.entity.enums.VerificationStatus;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PhoneVerifyMapper {

    /**
     * 인증 요청 생성 (Service에서 계산된 값들을 받아 Entity 생성)
     * - status는 항상 PENDING으로 시작
     * - [수정] id, createdAt은 Builder 생성자에 포함되지 않으므로 ignore 설정도 제거해야 함
     */
    // @Mapping(target = "id", ignore = true)        <-- 삭제
    // @Mapping(target = "createdAt", ignore = true) <-- 삭제
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "authCode", source = "authCode")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "status", constant = "PENDING") // 기본값 고정
    PhoneVerifyRequest toEntity(String phoneNumber, String authCode, LocalDateTime expiresAt);

    /**
     * 인증 상태 응답 (Enum -> DTO)
     */
    default PhoneVerifyStatusDto toStatusDto(VerificationStatus status) {
        return new PhoneVerifyStatusDto(status);
    }

    /**
     * 인증 상태 응답 (String -> DTO)
     */
    default PhoneVerifyStatusDto toStatusDto(String status) {
        return new PhoneVerifyStatusDto(status);
    }

    /**
     * 인증 요청 응답 (AuthCode -> DTO)
     */
    default PhoneVerifyResponseDto toResponseDto(String authCode) {
        return new PhoneVerifyResponseDto(authCode);
    }
}