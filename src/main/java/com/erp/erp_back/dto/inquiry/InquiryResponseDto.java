package com.erp.erp_back.dto.inquiry;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponseDto {
    private Long inquiryId; 
    private String ownerName; 
    private String storeName; 
    private InquiryCategory category;
    private String title;
    private String content;
    private String answer;
    private String adminName;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
}