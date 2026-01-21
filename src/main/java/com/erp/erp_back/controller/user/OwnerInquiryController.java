package com.erp.erp_back.controller.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ✅ 추가
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.service.inquiry.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/inquiries") // 사장님 전용 경로
@RequiredArgsConstructor
public class OwnerInquiryController {

    private final InquiryService inquiryService;

    // 1. 문의 등록
    @PostMapping
    public ResponseEntity<Void> createInquiry(
            @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 토큰에서 ID 추출
            @Valid @RequestBody InquiryRequestDto.Create request
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        inquiryService.createInquiry(ownerId, request);
        return ResponseEntity.ok().build();
    }

    // 2. 내 문의 내역 조회
    @GetMapping
    public ResponseEntity<Page<InquiryResponseDto>> getMyInquiries(
            @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 토큰에서 ID 추출
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        return ResponseEntity.ok(inquiryService.getMyInquiries(ownerId, pageable));
    }

    // 3. 내 문의 내역 삭제
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(
            @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 토큰에서 ID 추출
            @PathVariable Long inquiryId
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        inquiryService.deleteInquiry(ownerId, inquiryId);
        return ResponseEntity.ok().build();
    }
}