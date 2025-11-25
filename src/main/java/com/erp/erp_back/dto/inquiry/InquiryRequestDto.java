package com.erp.erp_back.dto.inquiry;

import com.erp.erp_back.entity.enums.InquiryCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InquiryRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotNull(message = "카테고리는 필수입니다.")
        private InquiryCategory category;

        @NotBlank(message = "제목은 필수입니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        private Long storeId;
    }

    @Getter
    @NoArgsConstructor
    public static class Answer {
        @NotBlank(message = "답변 내용은 필수입니다.")
        private String answer;
    }
}