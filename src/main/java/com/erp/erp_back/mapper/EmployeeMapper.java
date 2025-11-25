package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.user.Employee;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeMapper {
    
    EmployeeResponse toResponse(Employee employee);
}