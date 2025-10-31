package com.erp.erp_back.scheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.hr.EmployeeDocument;
import com.erp.erp_back.repository.hr.EmployeeDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j // 로그 사용을 위해 추가
@Component // Spring이 이 클래스를 Bean으로 관리하도록
@RequiredArgsConstructor // final 필드 생성자 자동 주입
public class FileCleanupScheduler {

    private final EmployeeDocumentRepository documentRepository;

    /**
     * 매일 새벽 1시에 실행됩니다. (초 분 시 일 월 요일)
     * "0 0 1 * * ?"
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void deleteExpiredDocuments() {
        log.info("만료된 인사 문서 삭제 스케줄러 시작...");
        
        LocalDate today = LocalDate.now();
        // 1. 오늘 날짜 기준으로 만료된 DB 데이터 조회
        List<EmployeeDocument> expiredDocuments = documentRepository.findAllByRetentionEndDateBefore(today);

        if (expiredDocuments.isEmpty()) {
            log.info("삭제할 만료된 문서가 없습니다.");
            return;
        }

        log.warn("총 {}개의 만료된 문서를 삭제합니다.", expiredDocuments.size());

        for (EmployeeDocument document : expiredDocuments) {
            try {
                // 2. 실제 파일 경로(filePath)를 가져와 Path 객체로 변환
                Path filePath = Paths.get(document.getFilePath());

                // 3. /uploads 폴더에서 실제 파일 삭제
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("파일 삭제 성공: {}", document.getFilePath());
                } else {
                    log.warn("파일이 이미 존재하지 않습니다: {}", document.getFilePath());
                }

                // 4. 파일 삭제 성공 시, DB의 메타데이터도 삭제
                documentRepository.delete(document);
                log.info("DB 데이터 삭제 성공: ID {}", document.getDocumentId());

            } catch (IOException e) {
                log.error("파일 삭제 중 오류 발생: {}", document.getFilePath(), e);
            } catch (Exception e) {
                log.error("DB 삭제 또는 기타 오류 발생: ID {}", document.getDocumentId(), e);
            }
        }
        log.info("만료된 인사 문서 삭제 스케줄러 완료.");
    }
}
