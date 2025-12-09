package com.erp.erp_back.repository.subscripition;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.subscripition.OwnerSubscription;

@Repository
public interface OwnerSubscriptionRepository extends JpaRepository<OwnerSubscription, Long> {

    /** (Admin) 특정 날짜 기준 활성 구독 수 */
    @Query("SELECT COUNT(os) FROM OwnerSubscription os WHERE os.expiryDate >= :date")
    long countActiveSubscriptions(@Param("date") LocalDate date);

    boolean existsBySubscriptionSubIdAndExpiryDateAfter(Long subId, LocalDate date);

    /** (Admin) 구독 현황 페이징 조회 */
    @Query("SELECT os FROM OwnerSubscription os " +
           "JOIN FETCH os.owner o " +
           "JOIN FETCH os.subscription s " +
           "WHERE (:q = '' OR o.email LIKE %:q% OR o.username LIKE %:q% OR s.subName LIKE %:q%)")
    Page<OwnerSubscription> findAdminOwnerSubscriptions(@Param("q") String q, Pageable pageable);

    Optional<OwnerSubscription> findFirstByOwnerOwnerIdAndExpiryDateAfter(Long ownerId, LocalDate date);

    /**
     * ✅ [수정] 사장님의 현재 구독 정보를 조회할 때, Subscription(상품명, 가격) 정보를 
     * JOIN FETCH로 함께 가져와서 DB 기반 데이터를 보장합니다.
     */
    @EntityGraph(attributePaths = {"subscription"}) 
    Optional<OwnerSubscription> findFirstByOwner_OwnerIdOrderByExpiryDateDesc(Long ownerId);
}