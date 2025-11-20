package com.erp.erp_back.repository.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.inquiry.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 사장님: 내 문의 내역 조회 (최신순)
    Page<Inquiry> findByOwner_OwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    // 관리자: 전체 조회 (최신순)
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자: 상태별 조회 (예: 답변 대기중인 것만 필터링)
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);
}