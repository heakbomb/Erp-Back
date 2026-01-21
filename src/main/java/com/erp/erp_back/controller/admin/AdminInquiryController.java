package com.erp.erp_back.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 시큐리티 사용 시
import org.springframework.security.core.userdetails.UserDetails; // 시큐리티 사용 시
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.service.inquiry.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    // 1. 전체 문의 조회 (필터링 가능) - [완벽합니다]
    // required = false 덕분에 프론트에서 undefined(파라미터 없음)로 보내도 null로 잘 받아집니다.
    @GetMapping
    public ResponseEntity<Page<InquiryResponseDto>> getAllInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryCategory category,
            @PageableDefault(size = 10) Pageable pageable) {
        
        return ResponseEntity.ok(inquiryService.getAllInquiries(status, category, pageable));
    }

    // 2. 답변 등록 - [수정됨]
    // Frontend가 POST를 쓰므로 @PostMapping이어야 합니다.
    @PostMapping("/{inquiryId}/reply")
    public ResponseEntity<Void> replyInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryRequestDto.Answer request
            // @AuthenticationPrincipal UserDetails userDetails // [추천] 토큰에서 ID 추출
    ) {
        // [중요] 프론트엔드에서 adminId를 파라미터로 안 보내고 있으므로,
        // 1. 시큐리티를 쓴다면 토큰에서 adminId를 꺼내야 합니다.
        // Long adminId = Long.parseLong(userDetails.getUsername()); 
        
        // 2. 만약 시큐리티 설정이 아직이고 테스트 중이라면 임시로 1번 관리자로 고정하거나, 
        //    프론트엔드 inquiryApi.ts를 수정해서 adminId를 넘겨줘야 합니다.
        
        Long adminId = 1L; // 임시 고정 (시큐리티 적용 전 테스트용)

        inquiryService.replyInquiry(adminId, inquiryId, request);
        return ResponseEntity.ok().build();
    }
}