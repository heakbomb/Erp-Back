package com.erp.erp_back.mapper;

import java.time.LocalDate;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.hr.EmployeeShiftBulkRequest;
import com.erp.erp_back.dto.hr.EmployeeShiftResponse;
import com.erp.erp_back.dto.hr.EmployeeShiftUpsertRequest;
import com.erp.erp_back.entity.hr.EmployeeShift;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeShiftMapper {

    // Entity -> Response DTO
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "employee.employeeId", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    EmployeeShiftResponse toResponse(EmployeeShift entity);

    // 생성 (단건): Request + Store + Employee -> Entity
    @Mapping(target = "shiftId", ignore = true) // 생성 시 ID 자동
    @Mapping(target = "store", source = "store")
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "breakMinutes", source = "req.breakMinutes", defaultValue = "0")
    @Mapping(target = "isFixed", source = "req.isFixed", defaultValue = "false")
    EmployeeShift toEntity(EmployeeShiftUpsertRequest req, Store store, Employee employee);

    // 수정 (단건): Request -> Entity 업데이트
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "shiftId", ignore = true) // ID는 변경 불가
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "breakMinutes", source = "req.breakMinutes") 
    @Mapping(target = "isFixed", source = "req.isFixed")
    void updateFromDto(EmployeeShiftUpsertRequest req, @MappingTarget EmployeeShift entity);

    // 생성 (Bulk): Request(시간) + 날짜(loop) -> Entity
    @Mapping(target = "shiftId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "shiftDate", source = "shiftDate") // 루프 변수 매핑
    @Mapping(target = "startTime", source = "req.startTime")
    @Mapping(target = "endTime", source = "req.endTime")
    @Mapping(target = "breakMinutes", source = "req.breakMinutes", defaultValue = "0")
    @Mapping(target = "isFixed", source = "req.isFixed", defaultValue = "false")
    EmployeeShift toEntityFromBulk(EmployeeShiftBulkRequest req, Store store, Employee employee, LocalDate shiftDate);
}