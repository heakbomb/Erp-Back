// src/main/java/com/erp/erp_back/controller/store/BusinessNumberController.java
package com.erp.erp_back.controller.store;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
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
                        @NotBlank String phone) {
        }

        @PostMapping("/verify")
        public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
                String bizNo = body.get("bizNo");
                String phone = body.getOrDefault("phone", "").trim();

                BusinessNumber saved = service.verifyAndSave(bizNo, phone);

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
}
