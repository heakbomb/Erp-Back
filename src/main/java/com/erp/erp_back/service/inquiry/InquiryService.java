package com.erp.erp_back.service.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.inquiry.Inquiry;
import com.erp.erp_back.entity.user.Admin;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.repository.inquiry.InquiryRepository;
import com.erp.erp_back.repository.user.AdminRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final OwnerRepository ownerRepository;
    private final AdminRepository adminRepository;

    // [사장님] 문의 등록
    @Transactional
    public void createInquiry(Long ownerId, InquiryRequestDto.Create request) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Inquiry inquiry = Inquiry.builder()
                .owner(owner)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .status(InquiryStatus.PENDING) // 기본값 명시
                .build();

        inquiryRepository.save(inquiry);
    }

    // [사장님] 내 문의 내역 조회
    public Page<InquiryResponseDto> getMyInquiries(Long ownerId, Pageable pageable) {
        return inquiryRepository.findByOwner_OwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(InquiryResponseDto::from);
    }

    // [관리자] 전체 문의 조회 (상태 필터링 옵션)
    public Page<InquiryResponseDto> getAllInquiries(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> page;
        if (status != null) {
            page = inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(InquiryResponseDto::from);
    }

    // [관리자] 답변 등록/수정
    @Transactional
    public void replyInquiry(Long adminId, Long inquiryId, InquiryRequestDto.Answer request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        inquiry.reply(admin, request.getAnswer());
        // Dirty Checking으로 자동 저장됨
    }
}