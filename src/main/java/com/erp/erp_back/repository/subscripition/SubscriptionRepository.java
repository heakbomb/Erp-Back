package com.erp.erp_back.repository.subscripition;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.subscripition.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    /**
     * ✅ [신규] (Admin) 구독 상품 페이징 및 검색/필터링 쿼리
     */
    @Query("SELECT s FROM Subscription s " +
           // ⭐️ [수정] :status = 'PRODUCTS' 조건을 'ALL'과 동일하게 추가합니다.
           "WHERE ( (:status = 'ALL' OR :status = 'PRODUCTS') OR " + 
           "        (:status = 'ACTIVE' AND s.isActive = true) OR " +
           "        (:status = 'INACTIVE' AND s.isActive = false) ) " +
           "AND (:q = '' OR s.subName LIKE %:q%)")
    Page<Subscription> findAdminSubscriptions(
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );

    // ✅ [신규] 사장님용: 활성화된 구독 상품 리스트 조회
    List<Subscription> findAllByIsActiveTrue();
}