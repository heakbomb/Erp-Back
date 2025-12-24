package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.subscription.AdminOwnerSubscriptionResponse;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
import com.erp.erp_back.entity.subscripition.OwnerSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerSubscriptionMapper {

    // 사장님용 응답 (Full DTO)
    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "subscription.subId", target = "subId")
    @Mapping(source = "subscription.subName", target = "subName")
    @Mapping(source = "subscription.monthlyPrice", target = "monthlyPrice")
    // [수정] subscription.isActive (X) -> subscription.active (O)
    @Mapping(source = "subscription.active", target = "isActive") 
    OwnerSubscriptionResponse toResponse(OwnerSubscription entity);

    // 관리자용 응답
    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "owner.username", target = "ownerName")
    @Mapping(source = "owner.email", target = "ownerEmail")
    @Mapping(source = "subscription.subId", target = "subId")
    @Mapping(source = "subscription.subName", target = "subName")
    AdminOwnerSubscriptionResponse toAdminResponse(OwnerSubscription entity);
}