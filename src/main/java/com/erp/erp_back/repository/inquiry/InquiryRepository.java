package com.erp.erp_back.repository.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.inquiry.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // 상태별 카운트 조회
    long countByStatus(InquiryStatus status);

    // 사장님용 (Owner Fetch)
    @Query("SELECT i FROM Inquiry i " +
           "JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.store " + 
           "WHERE i.owner.ownerId = :ownerId ORDER BY i.createdAt DESC")
    Page<Inquiry> findByOwner_OwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId, Pageable pageable);

    // 관리자용 (Owner + Admin Fetch)
    @Query("SELECT i FROM Inquiry i " +
           "JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.store " +    
           "LEFT JOIN FETCH i.admin " +      
           "ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllWithFetch(Pageable pageable);

    // 관리자용 필터링 (Owner + Admin Fetch)
    // 파라미터가 NULL이면 해당 조건은 무시(전체 조회)됩니다.
    @Query("SELECT i FROM Inquiry i " +
           "JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.store " +
           "LEFT JOIN FETCH i.admin " +
           "WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:category IS NULL OR i.category = :category) " +
           "ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllByFilters(@Param("status") InquiryStatus status,
                                   @Param("category") InquiryCategory category,
                                   Pageable pageable);
}