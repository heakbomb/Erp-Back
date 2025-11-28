package com.erp.erp_back.repository.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.user.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    // 특정 사장님의 모든 카드 조회
    List<PaymentMethod> findAllByOwner_OwnerId(Long ownerId);
    
    // 기본 카드 조회
    Optional<PaymentMethod> findFirstByOwner_OwnerIdAndIsDefaultTrue(Long ownerId);
    
    // 본인 카드인지 확인용
    Optional<PaymentMethod> findByPaymentIdAndOwner_OwnerId(Long paymentId, Long ownerId);
}