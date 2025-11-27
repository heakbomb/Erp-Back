package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.user.Employee;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

    // 조회용: Entity -> Response DTO
    EmployeeResponse toResponse(Employee employee);

    // 수정용: DTO -> Entity 업데이트 (null 필드 무시)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "provider", ignore = true)   // 소셜 제공자 변경 불가
    @Mapping(target = "providerId", ignore = true) // 소셜 ID 변경 불가
    @Mapping(target = "createdAt", ignore = true)
    void updateFromDto(EmployeeResponse dto, @MappingTarget Employee employee);
}