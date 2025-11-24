package com.erp.erp_back.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.service.inquiry.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/inquiries") // 관리자 전용 경로
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    // 1. 전체 문의 조회 (필터링 가능)
    @GetMapping
    public ResponseEntity<Page<InquiryResponseDto>> getAllInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryCategory category, // [추가]
            @PageableDefault(size = 10) Pageable pageable) {
        
        return ResponseEntity.ok(inquiryService.getAllInquiries(status, category, pageable));
    }
    // 2. 답변 등록
    @PutMapping("/{inquiryId}/reply")
    public ResponseEntity<Void> replyInquiry(@RequestParam Long adminId,
                                             @PathVariable Long inquiryId,
                                             @Valid @RequestBody InquiryRequestDto.Answer request) {
        inquiryService.replyInquiry(adminId, inquiryId, request);
        return ResponseEntity.ok().build();
    }
}