package com.erp.erp_back.service.hr;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.PayrollHistoryDetailDto;
import com.erp.erp_back.entity.hr.PayrollRun;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.hr.PayrollRunRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollHistoryService payrollHistoryService;

    /**
     * ✅ 수동 재계산 (사장이 버튼 누름)
     * - payroll_history 업서트
     * - payroll_run 은 DRAFT 유지 (말일 스케줄러 FINALIZED와 분리)
     * - version++ / calculatedAt 갱신
     */
    public List<PayrollHistoryDetailDto> manualRecalculate(Long storeId, YearMonth yearMonth) {
        String ym = yearMonth.toString(); // "2025-12"
        PayrollRun run = lockOrCreateRun(storeId, ym);

        // 마감 정책: FINALIZED면 재계산 막기(권장)
        if ("FINALIZED".equals(run.getStatus())) {
            throw new IllegalStateException("이미 마감된 월입니다. (payrollMonth=" + ym + ")");
        }

        // ✅ 계산 + payroll_history 저장(업서트)
        List<PayrollHistoryDetailDto> result =
                payrollHistoryService.saveMonthlyHistory(storeId, yearMonth);

        // ✅ run 업데이트 (DRAFT 유지)
        run.setStatus("DRAFT");
        run.setSource("MANUAL");
        run.setVersion(run.getVersion() + 1);
        run.setCalculatedAt(LocalDateTime.now());

        payrollRunRepository.save(run);
        return result;
    }

    /**
     * ✅ 말일 자동 마감 (스케줄러)
     * - 스케줄러는 "무조건 실행"되지만,
     * - 해당 월이 FINALIZED면 내부에서 작업 스킵(return)
     */
    public void autoFinalizeIfNeeded(Long storeId, YearMonth yearMonth) {
        String ym = yearMonth.toString();
        PayrollRun run = lockOrCreateRun(storeId, ym);

        // 이미 마감이면 스킵
        if ("FINALIZED".equals(run.getStatus())) {
            return;
        }

        try {
            // ✅ 계산 + payroll_history 저장(업서트)
            payrollHistoryService.saveMonthlyHistory(storeId, yearMonth);

            // ✅ 마감 처리
            run.setStatus("FINALIZED");
            run.setSource("AUTO");
            run.setVersion(run.getVersion() + 1);
            LocalDateTime now = LocalDateTime.now();
            run.setCalculatedAt(now);
            run.setFinalizedAt(now);

            payrollRunRepository.save(run);
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setSource("AUTO");
            payrollRunRepository.save(run);
            throw e;
        }
    }

    /**
     * ✅ (컨트롤러에서 호출하는 수동 실행 엔트리 포인트)
     * - 기존엔 payroll_history만 저장해서 payroll_run이 비어있었음
     * - 이제는 manualRecalculate를 호출해서 payroll_run도 반드시 남기도록 수정
     */
    @Transactional
    public List<PayrollHistoryDetailDto> runManual(Long storeId, YearMonth yearMonth) {
        return manualRecalculate(storeId, yearMonth);
    }

    /**
     * ✅ (추가) 특정 매장 + 월의 PayrollRun을 조회하되, 없으면 null 반환
     * - "버튼 비활성화 / 마감 여부 표시" 같은 프론트 판단용으로 사용 가능
     * - lock 걸지 않고 단순 조회
     */
    @Transactional(readOnly = true)
    public PayrollRun getRunOrNull(Long storeId, YearMonth yearMonth) {
        String ym = yearMonth.toString(); // "2025-12"
        return payrollRunRepository.findByStore_StoreIdAndPayrollMonth(storeId, ym)
                .orElse(null);
    }

    private PayrollRun lockOrCreateRun(Long storeId, String payrollMonth) {
        return payrollRunRepository.findForUpdate(storeId, payrollMonth)
                .orElseGet(() -> {
                    Store storeRef = new Store();
                    storeRef.setStoreId(storeId);

                    PayrollRun created = PayrollRun.builder()
                            .store(storeRef)
                            .payrollMonth(payrollMonth)
                            .status("DRAFT")
                            .version(0)
                            .build();

                    // ⚠️ 동시 생성 레이스는 유니크 제약(uk_payroll_run_store_month)로 최종 방어
                    return payrollRunRepository.save(created);
                });
    }
}