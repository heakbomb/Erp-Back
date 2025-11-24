package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.subscription.SubscriptionResponse;
import com.erp.erp_back.entity.subscripition.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    // ⭐️ [수정] Entity의 active -> DTO의 isActive
    @Mapping(source = "active", target = "isActive")
    SubscriptionResponse toResponse(Subscription entity);
}