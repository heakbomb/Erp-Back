package com.erp.erp_back.dto.inquiry;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.inquiry.Inquiry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponseDto {
    private Long inquiryId;
    private String ownerName; // 사장님 이름
    private String storeName; // 대표 사업장 이름 (필요시 추가 로직 필요, 여기선 사장님 이름만)
    private InquiryCategory category;
    private String title;
    private String content;
    private String answer;
    private String adminName; // 답변자 이름
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    public static InquiryResponseDto from(Inquiry inquiry) {
        return InquiryResponseDto.builder()
                .inquiryId(inquiry.getId())
                .ownerName(inquiry.getOwner().getUsername()) // Owner 엔티티 필드명 확인 필요 (username 가정)
                .category(inquiry.getCategory())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .adminName(inquiry.getAdmin() != null ? inquiry.getAdmin().getUsername() : null)
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .answeredAt(inquiry.getAnsweredAt())
                .build();
    }
}