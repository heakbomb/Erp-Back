package com.erp.erp_back.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.admin.AdminOwnerDetailResponse;
import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.dto.admin.AdminUserStatsResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.mapper.AdminMapper;
import com.erp.erp_back.mapper.OwnerSubscriptionMapper;
import com.erp.erp_back.mapper.StoreMapper;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
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
    private final OwnerSubscriptionRepository ownerSubscriptionRepository;

    private final AdminMapper adminMapper;
    private final StoreMapper storeMapper;
    private final OwnerSubscriptionMapper ownerSubscriptionMapper;

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
        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();

        LocalDateTime now = LocalDateTime.now();

        return storeRepository.findDashboardItemsByOwnerId(ownerId, startOfMonth, now);
    }

    /**
     * 3. (Admin) 사장님 상세 정보 조회
     * - Builder 패턴 제거 -> Mapper 사용
     */
    public AdminOwnerDetailResponse getOwnerDetail(Long ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<StoreSimpleResponse> stores = storeRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(storeMapper::toSimpleResponse)
                .toList();

        OwnerSubscriptionResponse subscription = ownerSubscriptionRepository.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .map(ownerSubscriptionMapper::toResponse)
                .orElse(null);

        // ✅ 수정됨: Mapper를 통해 변환 (Builder 제거)
        return adminMapper.toOwnerDetailResponse(owner, stores, subscription);
    }
}