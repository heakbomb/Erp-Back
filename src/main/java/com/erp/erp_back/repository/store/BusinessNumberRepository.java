package com.erp.erp_back.repository.store;

import java.util.List;              // ✅ 추가
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.store.BusinessNumber;

@Repository
public interface BusinessNumberRepository extends JpaRepository<BusinessNumber, Long> {

    Optional<BusinessNumber> findByBizNum(String bizNum);

   // ✅ ownerId 기준 + 인증 완료 + 폐업자 제외
    List<BusinessNumber> findByOwner_OwnerIdAndCertifiedAtIsNotNullAndOpenStatusNot(
            Long ownerId,
            String openStatus
    );

    // ✅ owner_id 로 사업자번호 목록 조회
    List<BusinessNumber> findByOwner_OwnerId(Long ownerId);
}