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
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "authCode", source = "authCode")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "status", constant = "PENDING") // 기본값 고정
    PhoneVerifyRequest toEntity(String phoneNumber, String authCode, LocalDateTime expiresAt);

    /**
     * 인증 상태 응답 (Enum -> DTO)
     * - DTO 생성자 로직(Enum.toString()) 활용을 위해 default 메서드 사용
     */
    default PhoneVerifyStatusDto toStatusDto(VerificationStatus status) {
        return new PhoneVerifyStatusDto(status);
    }

    /**
     * 인증 상태 응답 (String -> DTO)
     * - "NOT_FOUND" 등 커스텀 문자열 처리용
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