package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.entity.log.AttendanceLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceLogMapper {

    @Mapping(source = "employee.employeeId", target = "employeeId")
    @Mapping(source = "store.storeId", target = "storeId")
    AttendanceLogResponse toResponse(AttendanceLog entity);
}