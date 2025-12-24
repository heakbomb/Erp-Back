package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.subscription.SubscriptionResponse;
import com.erp.erp_back.entity.subscripition.Subscription;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    // ✅ source="active"는 Entity 필드명, target="isActive"는 DTO 필드명
    @Mapping(source = "active", target = "isActive")
    SubscriptionResponse toResponse(Subscription entity);
}