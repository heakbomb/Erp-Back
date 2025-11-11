package com.erp.erp_back.service.log;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceQrService {

    private final StoreRepository storeRepository;
    private final AttendanceQrTokenRepository qrTokenRepository;

    // 60초짜리 QR 만드는 예시
    public AttendanceQrToken createQrForStore(Long storeId, int ttlSeconds) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장"));

        String random = UUID.randomUUID().toString().replace("-", "");
        String value = "ST" + storeId + "-" + random;

        AttendanceQrToken token = new AttendanceQrToken();
        token.setStore(store);
        token.setTokenValue(value);
        token.setExpireAt(LocalDateTime.now().plusSeconds(ttlSeconds));

        return qrTokenRepository.save(token);
    }

    // “현재 QR” 조회용
    public AttendanceQrToken getCurrentForStore(Long storeId) {
        return qrTokenRepository.findTopByStore_StoreIdOrderByExpireAtDesc(storeId)
                .orElseThrow(() -> new IllegalStateException("현재 QR이 없습니다. 새로 생성하세요."));
    }
}
