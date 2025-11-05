package com.erp.erp_back.repository.subscripition;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.subscripition.OwnerSubscription;

@Repository
public interface OwnerSubscriptionRepository extends JpaRepository<OwnerSubscription, Long> {
    /**  (Admin) 특정 날짜 기준 활성 구독 수 */
    @Query("SELECT COUNT(os) FROM OwnerSubscription os " +
           "WHERE os.expiryDate >= :date")
    long countActiveSubscriptions(@Param("date") LocalDate date);
    // 상품 삭제 방지용
    boolean existsBySubscriptionSubIdAndExpiryDateAfter(Long subId, LocalDate date);

    /** (Admin) 구독 현황 페이징 및 검색 쿼리
     * (사장님 이메일, 사장님 이름, 구독 상품명으로 검색)
     */
    @Query("SELECT os FROM OwnerSubscription os " +
           "JOIN FETCH os.owner o " +
           "JOIN FETCH os.subscription s " +
           "WHERE (:q = '' OR o.email LIKE %:q% OR o.username LIKE %:q% OR s.subName LIKE %:q%)")
    Page<OwnerSubscription> findAdminOwnerSubscriptions(
            @Param("q") String q,
            Pageable pageable
    );

    /** (Owner) 사장님이 현재 활성/만료예정인 구독이 있는지 확인 */
    Optional<OwnerSubscription> findFirstByOwnerOwnerIdAndExpiryDateAfter(Long ownerId, LocalDate date);
}