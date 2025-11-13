package com.erp.erp_back.service.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.dto.admin.AdminUserStatsResponse;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final OwnerRepository ownerRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final SalesTransactionRepository salesTransactionRepository;

    /**
     * 1. (Admin) 사용자 통계 KPI 조회
     */
    public AdminUserStatsResponse getAdminUserStats() {
        long totalOwners = ownerRepository.count();
        long totalEmployees = employeeRepository.count();

        // 이번 달 신규 가입자 수
        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        
        long newOwners = ownerRepository.countByCreatedAtAfter(startOfMonth);
        long newEmployees = employeeRepository.countByCreatedAtAfter(startOfMonth);

        return AdminUserStatsResponse.builder()
                .totalUsers(totalOwners + totalEmployees)
                .totalOwners(totalOwners)
                .totalEmployees(totalEmployees)
                .newSignupsThisMonth(newOwners + newEmployees)
                .build();
    }

    /**
     * 2. (Admin) 특정 사장님의 사업장 대시보드 목록 조회
     */
    public List<AdminStoreDashboardItem> getOwnerStoreDashboard(Long ownerId) {
        
        // 1. 해당 사장님의 모든 사업장 조회
        List<Store> stores = storeRepository.findAllByOwnerId(ownerId);

        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 2. 각 사업장별로 집계 데이터 계산
        return stores.stream().map(store -> {
            Long storeId = store.getStoreId();

            // 2-1. ⭐️ [수정] (집계) 승인된 직원 수 (메서드명 변경)
            long employeeCount = employeeAssignmentRepository
                    .countByStoreStoreIdAndStatus(storeId, "APPROVED"); // ⭐️ 수정됨

            // 2-2. (집계) 당월 매출
            BigDecimal totalSalesMonth = salesTransactionRepository
                    .sumSalesAmountByStoreIdBetween(storeId, startOfMonth, now);
            
            // 2-3. (집계) 최근 매출일 (findTopByStoreStoreId... 카멜케이스 사용)
            LocalDateTime lastSalesDate = salesTransactionRepository
                    .findTopByStoreStoreIdOrderByTransactionTimeDesc(storeId)
                    .map(SalesTransaction::getTransactionTime)
                    .orElse(null);

            return AdminStoreDashboardItem.builder()
                    .storeId(storeId)
                    .storeName(store.getStoreName())
                    .industry(store.getIndustry())
                    .status(store.getStatus())
                    .employeeCount(employeeCount)
                    .totalSalesMonth(totalSalesMonth)
                    .lastSalesDate(lastSalesDate)
                    .build();
        }).collect(Collectors.toList());
    }
}