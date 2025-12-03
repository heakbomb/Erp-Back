package com.erp.erp_back.service.erp;

import com.erp.erp_back.repository.erp.InventorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySnapshotService {

    private final InventorySnapshotRepository inventorySnapshotRepository;

    /**
     * 일별 재고 스냅샷 생성 (트랜잭션 관리)
     */
    @Transactional
    public void createSnapshotForDate(LocalDate targetDate) {
        // 1. 중복 체크
        if (inventorySnapshotRepository.existsBySnapshotDate(targetDate)) {
            log.warn("이미 {} 일자 재고 스냅샷이 존재합니다.", targetDate);
            return;
        }

        // 2. 스냅샷 생성 (Native Query 실행)
        inventorySnapshotRepository.createDailySnapshot(targetDate);
        log.info("{} 일자 재고 스냅샷 생성 완료.", targetDate);
    }
}