package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.hr.DocumentListResponseDto;
import com.erp.erp_back.dto.hr.FileUploadResponse;
import com.erp.erp_back.entity.hr.EmployeeDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    // 파일 업로드 직후 응답 변환
    FileUploadResponse toUploadResponse(EmployeeDocument entity);

    // 문서 목록 조회용 변환
    DocumentListResponseDto toListResponse(EmployeeDocument entity);
}