package com.erp.erp_back.service.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.inquiry.InquiryRequestDto;
import com.erp.erp_back.dto.inquiry.InquiryResponseDto;
import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.inquiry.Inquiry;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Admin;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.mapper.InquiryMapper;
import com.erp.erp_back.repository.inquiry.InquiryRepository;
import com.erp.erp_back.repository.store.StoreRepository;
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
    private final InquiryMapper inquiryMapper;
    private final StoreRepository storeRepository;

    // [사장님] 문의 등록
    @Transactional
    public void createInquiry(Long ownerId, InquiryRequestDto.Create request) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // storeId가 있으면 조회, 없으면 null
        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

            // (선택) 본인 사업장이 맞는지 검증 로직 추가 가능
            if (!store.getBusinessNumber().getOwner().getOwnerId().equals(ownerId)) {
                throw new IllegalArgumentException("본인의 사업장이 아닙니다.");
            }
        }
        Inquiry inquiry = inquiryMapper.toEntity(request, owner, store);

        inquiryRepository.save(inquiry);
    }

    // [사장님] 내 문의 내역 조회
    public Page<InquiryResponseDto> getMyInquiries(Long ownerId, Pageable pageable) {
        return inquiryRepository.findByOwner_OwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(inquiryMapper::toResponse);
    }

    // [사장님] 문의 삭제
    @Transactional
    public void deleteInquiry(Long ownerId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        // 1. 본인이 작성한 글인지 확인
        if (!inquiry.getOwner().getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("본인의 문의글만 삭제할 수 있습니다.");
        }

        // 2. 답변이 달린 상태면 삭제 불가 (선택 사항 - 정책에 따라 다름)
        if (inquiry.getStatus() == InquiryStatus.RESPONDED) {
            throw new IllegalArgumentException("이미 답변이 등록된 문의는 삭제할 수 없습니다.");
        }

        inquiryRepository.delete(inquiry);
    }

    // [관리자] 전체 문의 조회 (상태, 카테고리 필터링)
    public Page<InquiryResponseDto> getAllInquiries(InquiryStatus status, InquiryCategory category, Pageable pageable) {
        // Repository의 통합 메서드 호출
        return inquiryRepository.findAllByFilters(status, category, pageable)
                .map(inquiryMapper::toResponse);
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