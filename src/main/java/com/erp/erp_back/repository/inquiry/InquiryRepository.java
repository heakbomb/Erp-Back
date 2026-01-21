//
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
           "LEFT JOIN FETCH i.owner " +    // [수정] 안전하게 LEFT JOIN으로 변경
           "LEFT JOIN FETCH i.store " +    
           "LEFT JOIN FETCH i.admin " +      
           "ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllWithFetch(Pageable pageable);

    // [중요 수정] 관리자용 필터링
    // 1. JOIN FETCH i.owner -> LEFT JOIN FETCH i.owner (사용자 삭제되거나 없어도 문의는 보여야 함)
    // 2. countQuery 속성 추가 (페이징 시 개수 계산 쿼리 분리 필수)
    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.store " +
           "LEFT JOIN FETCH i.admin " +
           "WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:category IS NULL OR i.category = :category) " +
           "ORDER BY i.createdAt DESC",
           
           countQuery = "SELECT count(i) FROM Inquiry i " +
           "WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:category IS NULL OR i.category = :category)")
    Page<Inquiry> findAllByFilters(@Param("status") InquiryStatus status,
                                   @Param("category") InquiryCategory category,
                                   Pageable pageable);
}