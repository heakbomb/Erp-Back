package com.erp.erp_back.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.subscription.AdminOwnerSubscriptionResponse;
import com.erp.erp_back.service.subscription.OwnerSubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/subscriptions") // ✅ '구독 현황' API 루트
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminOwnerSubscriptionController {

    private final OwnerSubscriptionService ownerSubService;

    /**
     * (Admin) 전체 구독 현황 목록 조회
     * GET /admin/owner-subscriptions?q=test@&page=0
     */
    @GetMapping
    public ResponseEntity<Page<AdminOwnerSubscriptionResponse>> getAdminSubscriptions(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "expiryDate") Pageable pageable
    ) {
        return ResponseEntity.ok(ownerSubService.getAdminSubscriptions(q, pageable));
    }
}