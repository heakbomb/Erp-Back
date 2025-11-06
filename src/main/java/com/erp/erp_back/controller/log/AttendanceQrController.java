package com.erp.erp_back.controller.log;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.service.log.AttendanceQrService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance/qr")
@RequiredArgsConstructor
public class AttendanceQrController {

    private final AttendanceQrService qrService;

    // 새 QR 생성 (예: 60초짜리)
    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestParam Long storeId,
                                         @RequestParam(defaultValue = "60") int ttl) {
        var token = qrService.createQrForStore(storeId, ttl);
        return ResponseEntity.ok(token.getTokenValue());
    }

    // 현재 QR 조회
    @GetMapping("/current")
    public ResponseEntity<String> current(@RequestParam Long storeId) {
        var token = qrService.getCurrentForStore(storeId);
        return ResponseEntity.ok(token.getTokenValue());
    }
}
