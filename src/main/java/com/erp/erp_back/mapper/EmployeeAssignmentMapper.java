package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.auth.EmployeeAssignmentResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeAssignmentMapper {

    @Mapping(source = "employee.employeeId", target = "employeeId")
    @Mapping(source = "store.storeId", target = "storeId")
    EmployeeAssignmentResponse toResponse(EmployeeAssignment entity);
}