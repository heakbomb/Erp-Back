package com.erp.erp_back.service.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.admin.DashboardStatsResponse;
import com.erp.erp_back.dto.log.AuditLogResponse;
import com.erp.erp_back.entity.log.AuditLog;
import com.erp.erp_back.repository.log.AuditLogRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final StoreRepository storeRepository;
    private final OwnerRepository ownerRepository;
    private final EmployeeRepository employeeRepository;
    private final OwnerSubscriptionRepository ownerSubscriptionRepository; // ✅ 추가
    private final AuditLogRepository auditLogRepository; // ✅ 추가

    public DashboardStatsResponse getDashboardStats() {
        
        // 1. 사업장 수 (유지)
        long totalStores = storeRepository.count();

        // 2. 전체 사용자 수 (유지)
        long totalOwners = ownerRepository.count();
        long totalEmployees = employeeRepository.count();
        long totalUsers = totalOwners + totalEmployees;

        // 3.  활성 구독 수 (오늘 날짜 기준)
        long activeSubscriptions = ownerSubscriptionRepository.countActiveSubscriptions(LocalDate.now());

        // 4.  승인 대기 사업장 수
        long pendingStoreCount = storeRepository.countByStatus("PENDING");
        
        // 5.  최근 활동 (최근 5건)
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLogResponse> recentActivities = auditLogRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                .stream()
                .map(this::toAuditLogDto)
                .collect(Collectors.toList());

        // DTO 조립
        return DashboardStatsResponse.builder()
                .totalStores(totalStores)
                .totalUsers(totalUsers)
                .activeSubscriptions(activeSubscriptions) // ✅ 수정
                .pendingStoreCount(pendingStoreCount) // ✅ 수정
                .recentActivities(recentActivities) // ✅ 수정
                .build();
    }
    
    // AuditLog DTO 변환
    private AuditLogResponse toAuditLogDto(AuditLog log) {
        return AuditLogResponse.builder()
                .auditId(log.getAuditId())
                .userId(log.getUserId())
                .userType(log.getUserType())
                .actionType(log.getActionType())
                .targetTable(log.getTargetTable())
                .createdAt(log.getCreatedAt())
                .build();
    }
}