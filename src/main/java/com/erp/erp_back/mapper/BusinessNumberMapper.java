package com.erp.erp_back.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.infra.nts.dto.NtsStatusItem;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BusinessNumberMapper {
/**
     * Entity 생성: 입력값 + 국세청 응답 + 사장님 정보 -> BusinessNumber
     * * [FIX] NtsStatusItem에 @Context를 추가했습니다.
     * 이렇게 하면 MapStruct가 Record 내부를 분석하지 않고 단순히 메서드로 전달만 하기 때문에
     * Eclipse Compiler에서 발생하는 NPE 에러를 회피할 수 있습니다.
     */
    @Mapping(target = "bizId", ignore = true)
    @Mapping(target = "stores", ignore = true)
    @Mapping(target = "certifiedAt", expression = "java(java.time.LocalDateTime.now())") // 인증 시간 현재로 설정

    // 기본 정보 매핑
    @Mapping(source = "bizNum", target = "bizNum")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "owner", target = "owner")
    
    // @Context로 변경했으므로 item 필드에 대한 ignore 설정은 더 이상 필요하지 않습니다.
    BusinessNumber toEntity(String bizNum, String phone, @Context NtsStatusItem item, Owner owner);

    /**
     * MapStruct가 Record 내부 필드를 분석하다가 에러가 발생하므로,
     * 후처리(AfterMapping)에서 수동으로 값을 할당합니다.
     */
    @AfterMapping
    default void mapNtsItem(@Context NtsStatusItem item, @MappingTarget BusinessNumber target) {
        if (item == null) {
            return;
        }
        // Record의 접근자 메서드 직접 호출
        target.setOpenStatus(item.bStt());
        target.setTaxType(item.taxType());
        target.setStartDt(item.startDt());
        target.setEndDt(item.endDt());
    }
}