package com.erp.erp_back.controller.store;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.service.store.BusinessNumberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business-number") // ✅ 프론트 호출 경로와 통일
@RequiredArgsConstructor
public class BusinessNumberController {

    private final BusinessNumberService service;

    /**
     * 사업자번호 인증 + DB 저장
     * 요청 바디 예) { "bizNo": "123-45-67890", "phone": "010-1234-5678" }
     * 응답 예) { "bizId": 1, "bizNo": "1234567890", "openStatus": "계속사업자", "taxType": "부가가치세 일반과세자" }
     */
    @PostMapping("/verify") // ✅ POST /business-number/verify
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String bizNo = body.get("bizNo");
        String phone = body.getOrDefault("phone", "").trim(); // ✅ 추가: phone도 받음(필수)

        // ✅ 서비스 시그니처: verifyAndSave(String bizNo, String phone)
        BusinessNumber saved = service.verifyAndSave(bizNo, phone);

        return ResponseEntity.ok(Map.of(
                "bizId", saved.getBizId(),
                "bizNo", saved.getBizNum(),
                "openStatus", saved.getOpenStatus(),
                "taxType", saved.getTaxType()
        ));
    }

    /**
     * 드롭다운용 목록
     * 응답 예) [ { "bizId":1, "bizNo":"1234567890", "openStatus":"계속사업자" }, ... ]
     */
    @GetMapping // ✅ GET /business-number
    public ResponseEntity<List<Map<String, Object>>> list() {
        var items = service.list().stream()
                .map(b -> Map.<String, Object>of(
                        "bizId", b.getBizId(),
                        "bizNo", b.getBizNum(),
                        "openStatus", b.getOpenStatus()
                ))
                .toList();
        return ResponseEntity.ok(items);
    }
}