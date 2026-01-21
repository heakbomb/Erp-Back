// src/main/java/com/erp/erp_back/controller/store/BusinessNumberController.java
package com.erp.erp_back.controller.store;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.service.store.BusinessNumberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business-number")
@RequiredArgsConstructor
public class BusinessNumberController {

    private final BusinessNumberService service;

    public record BizVerifyRequest(
            @NotBlank String bizNo,
            @NotBlank String phone
    ) {}

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String bizNo = body.get("bizNo");
        String phone = body.getOrDefault("phone", "").trim();

        // ✅ 로그인 ownerId 추출 (프론트에서 ownerId 받지 않음)
        Long ownerId = resolveOwnerId();

        BusinessNumber saved = service.verifyAndSave(ownerId, bizNo, phone);

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("bizId", saved.getBizId());
        resp.put("bizNo", saved.getBizNum());
        resp.put("openStatus", saved.getOpenStatus());
        resp.put("taxType", saved.getTaxType());
        resp.put("endDt", saved.getEndDt());

        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        var items = service.list().stream()
                .map(b -> Map.<String, Object>of(
                        "bizId", b.getBizId(),
                        "bizNo", b.getBizNum(),
                        "openStatus", b.getOpenStatus(),
                        "taxType", b.getTaxType()))
                .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * ✅ 현재 로그인한 Owner의 ownerId를 가져온다.
     * - 프로젝트에서 Authentication name이 "ownerId" 또는 "email"로 설정될 수 있으므로
     *   우선 숫자로 파싱 시도 -> 실패하면 서비스에서 email로 owner 조회하도록 확장 가능.
     *
     * 현재 문제는 ownerId=1 하드코딩이므로, "숫자 ownerId를 principal에 넣는 방식"이면 즉시 해결된다.
     */
    private Long resolveOwnerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        // 1) auth.getName()이 숫자 ownerId로 들어오는 경우(가장 간단)
        String name = auth.getName();
        if (name != null) {
            try {
                long id = Long.parseLong(name);
                if (id > 0) return id;
            } catch (NumberFormatException ignore) {
                // 아래에서 principal 기반 추출 시도
            }
        }

        // 2) principal에서 ownerId 추출 (CustomUserDetails가 있으면 여기에 맞춰 캐스팅)
        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        // ✅ 여기 캐스팅은 프로젝트 사용자 상세 클래스에 맞춰 1개만 남기면 됨
        // 예: CustomOwnerDetails, JwtPrincipal 등
        try {
            // (예시) principal에 getOwnerId()가 있는 경우
            var m = principal.getClass().getMethod("getOwnerId");
            Object v = m.invoke(principal);
            if (v instanceof Number n && n.longValue() > 0) return n.longValue();
        } catch (Exception ignore) {
            // fallthrough
        }

        throw new IllegalStateException("ownerId를 확인할 수 없습니다.");
    }
}