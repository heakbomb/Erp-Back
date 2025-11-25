package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.entity.inquiry.Inquiry;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Owner;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InquiryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "store", source = "store")
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "answer", ignore = true)
    @Mapping(target = "answeredAt", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    Inquiry toEntity(InquiryRequestDto.Create req, Owner owner, Store store);

    // ⭐️ [수정] Entity의 'id' -> DTO의 'inquiryId'로 매핑
    @Mapping(source = "id", target = "inquiryId")
    @Mapping(source = "owner.username", target = "ownerName")
    @Mapping(source = "admin.username", target = "adminName")
    @Mapping(source = "store.storeName", target = "storeName")
    InquiryResponseDto toResponse(Inquiry inquiry);
}