package com.erp.erp_back.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.dto.admin.AdminUserStatsResponse;
import com.erp.erp_back.mapper.AdminMapper;
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
        private final AdminMapper adminMapper;

        /**
         * 1. (Admin) 사용자 통계 KPI 조회
         */
        public AdminUserStatsResponse getAdminUserStats() {
                long totalOwners = ownerRepository.count();
                long totalEmployees = employeeRepository.count();

                LocalDateTime startOfMonth = LocalDate.now()
                                .with(TemporalAdjusters.firstDayOfMonth())
                                .atStartOfDay();

                long newOwners = ownerRepository.countByCreatedAtAfter(startOfMonth);
                long newEmployees = employeeRepository.countByCreatedAtAfter(startOfMonth);

                return adminMapper.toStatsResponse(
                                totalOwners + totalEmployees,
                                totalOwners,
                                totalEmployees,
                                newOwners + newEmployees);
        }

        /**
         * 2. (Admin) 특정 사장님의 사업장 대시보드 목록 조회
         */
        public List<AdminStoreDashboardItem> getOwnerStoreDashboard(Long ownerId) {

                // 이번 달 1일 00:00:00 계산
                LocalDateTime startOfMonth = LocalDate.now()
                                .with(TemporalAdjusters.firstDayOfMonth())
                                .atStartOfDay();

                // 현재 시간
                LocalDateTime now = LocalDateTime.now();

                return storeRepository.findDashboardItemsByOwnerId(ownerId, startOfMonth, now);
        }
}