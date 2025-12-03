package com.erp.erp_back.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class PartitionManager {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 매월 25일 새벽 03:00 실행
     * 목적: 다음 달이 오기 전에 미리 다음 달 파티션을 생성함
     */
    @Scheduled(cron = "0 0 3 25 * ?") 
    @Transactional
    public void autoCreateNextPartition() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1); // 다음 달 (예: 현재 1월이면 2월)
        
        // 파티션 이름: p202601, p202602 ...
        String partitionName = "p" + nextMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        
        // 파티션 기준일: 다음 달의 다음 달 1일 (예: 2월 파티션은 '3월 1일 미만'을 담아야 함)
        LocalDate limitDate = nextMonth.plusMonths(1).withDayOfMonth(1);
        String limitDateStr = limitDate.format(DateTimeFormatter.ISO_DATE); // "2026-03-01"

        log.info("[파티션 관리] {} 파티션 생성 시도 (기준: {} 미만)", partitionName, limitDateStr);

        try {
            // MySQL 명령어: p_future(미래분)를 쪼개서 [다음달 파티션] + [나머지 미래]로 나눔
            String sql = String.format(
                "ALTER TABLE inventory_snapshot REORGANIZE PARTITION p_future INTO (" +
                "    PARTITION %s VALUES LESS THAN ('%s'), " +
                "    PARTITION p_future VALUES LESS THAN (MAXVALUE)" +
                ")", partitionName, limitDateStr);

            entityManager.createNativeQuery(sql).executeUpdate();
            
            log.info("[파티션 관리] 성공! {} 파티션이 생성되었습니다.", partitionName);

        } catch (Exception e) {
            // 이미 존재하면 에러가 날 수 있으나 서비스엔 지장 없음
            log.warn("[파티션 관리] 파티션 생성 실패 (이미 존재할 수 있음): {}", e.getMessage());
        }
    }
    
    /**
     * (옵션) 매월 1일 새벽 03:30 실행
     * 목적: 3년(36개월) 지난 오래된 데이터 파티션 통째로 삭제 (용량 확보)
     */
    @Scheduled(cron = "0 30 3 1 * ?")
    @Transactional
    public void dropOldPartitions() {
        LocalDate oldDate = LocalDate.now().minusMonths(36); // 3년 전
        String partitionName = "p" + oldDate.format(DateTimeFormatter.ofPattern("yyyyMM"));

        try {
            String sql = "ALTER TABLE inventory_snapshot DROP PARTITION " + partitionName;
            entityManager.createNativeQuery(sql).executeUpdate();
            log.info("[파티션 정리] 3년 지난 파티션({}) 삭제 완료.", partitionName);
        } catch (Exception e) {
            log.warn("[파티션 정리] 삭제 실패 (이미 지워졌거나 없음): {}", e.getMessage());
        }
    }
}