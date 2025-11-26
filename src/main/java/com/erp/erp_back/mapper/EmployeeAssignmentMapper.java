package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.auth.EmployeeAssignmentRequest;
import com.erp.erp_back.dto.auth.EmployeeAssignmentResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeAssignmentMapper {

    @Mapping(source = "employee.employeeId", target = "employeeId")
    @Mapping(source = "store.storeId", target = "storeId")
    EmployeeAssignmentResponse toResponse(EmployeeAssignment entity);

    @Mapping(target = "assignmentId", ignore = true)
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "store", source = "store")
    @Mapping(target = "role", source = "req.role")
    @Mapping(target = "status", constant = "PENDING") // 기본값 PENDING 설정
    EmployeeAssignment toEntity(EmployeeAssignmentRequest req, Employee employee, Store store);
}