package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.log.AuditLogResponse;
import com.erp.erp_back.entity.log.AuditLog;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    // Entity -> DTO 변환
    AuditLogResponse toResponse(AuditLog entity);
}