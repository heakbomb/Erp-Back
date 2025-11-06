package com.erp.erp_back.repository.log;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.log.AttendanceQrToken;

public interface AttendanceQrTokenRepository extends JpaRepository<AttendanceQrToken, Long> {

    Optional<AttendanceQrToken> findTopByStore_StoreIdOrderByExpireAtDesc(Long storeId);

    Optional<AttendanceQrToken> findByTokenValue(String tokenValue);

    void deleteByStore_StoreId(Long storeId);
}