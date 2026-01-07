package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.user.Owner;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BusinessNumberMapper {

    @Mapping(target = "bizId", ignore = true)
    @Mapping(target = "stores", ignore = true)
    @Mapping(target = "certifiedAt", expression = "java(java.time.LocalDateTime.now())")

    @Mapping(source = "bizNum", target = "bizNum")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "owner", target = "owner")

    @Mapping(source = "openStatus", target = "openStatus")
    @Mapping(source = "taxType", target = "taxType")
    @Mapping(source = "endDt", target = "endDt")
    BusinessNumber toEntity(String bizNum, String phone,
                            String openStatus, String taxType, String endDt,
                            Owner owner);
}