package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.dto.user.OwnerResponse;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.entity.user.Owner;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // --- Employee 매핑 ---
    EmployeeResponse toEmployeeResponse(Employee employee);

    // 직원 수정용 (DTO -> Entity 업데이트)
    // providerId, createdAt 등은 변경되지 않도록 ignore 처리
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "providerId", ignore = true) // 변경 불가
    @Mapping(target = "createdAt", ignore = true)
    void updateEmployeeFromDto(EmployeeResponse dto, @MappingTarget Employee employee);

    // --- Owner 매핑 ---
    OwnerResponse toOwnerResponse(Owner owner);
}