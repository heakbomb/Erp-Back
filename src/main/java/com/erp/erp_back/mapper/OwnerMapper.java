package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.user.OwnerResponse;
import com.erp.erp_back.entity.user.Owner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerMapper {

    // Owner Entity -> OwnerResponse DTO 변환
    OwnerResponse toResponse(Owner owner);
}