package com.erp.erp_back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.BusinessNumber;

@Repository
public interface BusinessNumberRepository extends JpaRepository<BusinessNumber, Long> {
    // 기본적인 CRUD 메소드가 이미 모두 구현되어 있음
    Optional<BusinessNumber> findByBizNum(String bizNum);
}