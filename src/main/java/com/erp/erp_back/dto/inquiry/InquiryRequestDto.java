package com.erp.erp_back.dto.inquiry;

import com.erp.erp_back.entity.enums.InquiryCategory;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class InquiryRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        private InquiryCategory category;
        private String title;
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class Answer {
        private String answer;
    }
}