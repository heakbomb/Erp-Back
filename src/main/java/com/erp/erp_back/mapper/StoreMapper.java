package com.erp.erp_back.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.log.StoreQrResponse;
import com.erp.erp_back.dto.store.BusinessNumberResponse;
import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StoreMapper {

    /**
     * Service에서 호출할 메인 메서드
     * 여러 Entity를 받아서 하나의 DTO로 합칩니다.
     */
    // 매장 생성: Request DTO + BusinessNumber -> Store Entity 변환
    @Mapping(target = "storeId", ignore = true) // ID는 자동 생성되므로 무시
    @Mapping(target = "status", constant = "PENDING") // 기본 상태 설정
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(source = "bn", target = "businessNumber") // 연관관계 매핑
    Store toEntity(StoreCreateRequest request, BusinessNumber bn);

    // 매장 수정: Request DTO의 내용으로 기존 Store Entity 업데이트
    // null이 아닌 필드만 업데이트하도록 설정 (NullValuePropertyMappingStrategy.IGNORE)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "status", ignore = true) // 상태는 별도 API로 변경하므로 무시
    @Mapping(target = "businessNumber", ignore = true) // 사업자 번호 변경 로직은 별도로 처리하거나 여기서 제외
    void updateFromDto(StoreCreateRequest request, @MappingTarget Store store);

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.status", target = "status")
    @Mapping(source = "store.approvedAt", target = "approvedAt")
    // ✅ 추가: active 매핑
    @Mapping(source = "store.active", target = "active")
    // BusinessNumber 내부 필드 접근 (Dot notation 사용)
    @Mapping(source = "store.businessNumber.bizId", target = "bizId")
    @Mapping(source = "store.businessNumber.bizNum", target = "bizNum")
    @Mapping(source = "store.businessNumber.owner.username", target = "ownerName")
    @Mapping(source = "store.businessNumber.owner.email", target = "ownerEmail")
    @Mapping(source = "store.businessNumber.openStatus", target = "openStatus")
    @Mapping(source = "store.businessNumber.taxType", target = "taxType")
    @Mapping(source = "store.businessNumber.phone", target = "phone")
    // StoreGps 매핑 (null일 경우 MapStruct가 알아서 null 처리)
    @Mapping(source = "gps.latitude", target = "latitude")
    @Mapping(source = "gps.longitude", target = "longitude")
    // 직원 목록 매핑
    @Mapping(source = "assignments", target = "employees")
    StoreResponse toResponse(Store store, StoreGps gps, List<EmployeeAssignment> assignments);

    // QR 토큰 엔티티 -> QR 응답 DTO 변환
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "tokenValue", target = "qrToken") // 필드명이 달라서 매핑 필요
    @Mapping(source = "expireAt", target = "expireAt")
    StoreQrResponse toQrResponse(AttendanceQrToken qrToken);

    // GPS가 없는 목록 조회용 (오버로딩)
    default StoreResponse toResponse(Store store) {
        return toResponse(store, null, null);
    }

    default StoreResponse toResponse(Store store, StoreGps gps) {
        return toResponse(store, gps, null);
    }

    // Store -> StoreSimpleResponse 변환
    @Mapping(source = "businessNumber.bizNum", target = "bizNum") // 필요한 필드 매핑 확인
    StoreSimpleResponse toSimpleResponse(Store store);

    /**
     * List<EmployeeAssignment> -> List<StoreEmployeeDto> 변환 시
     * MapStruct가 이 메서드를 자동으로 찾아 사용합니다.
     */
    @Mapping(source = "employee.name", target = "name")
    StoreResponse.StoreEmployeeDto toEmployeeDto(EmployeeAssignment assignment);

    // ✅ 추가: BusinessNumber -> DTO 변환
    @Mapping(source = "owner.ownerId", target = "ownerId")
    BusinessNumberResponse toBusinessNumberResponse(BusinessNumber entity);
}