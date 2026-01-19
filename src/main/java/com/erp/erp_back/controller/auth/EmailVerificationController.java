package com.erp.erp_back.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.entity.enums.VerificationStatus;
import com.erp.erp_back.service.auth.EmailVerificationService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService service;

    @PostMapping
    public ResponseEntity<SendResp> send(@RequestBody SendReq req) {
        String id = service.send(req.getEmail());
        return ResponseEntity.ok(new SendResp(id));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<Void> resend(@PathVariable("id") String id) {
        service.resend(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResp> confirm(@RequestBody ConfirmReq req) {
        boolean ok = service.confirm(req.getVerificationId(), req.getCode());
        return ResponseEntity.ok(new ConfirmResp(ok));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusResp> status(@PathVariable("id") String id) {
        VerificationStatus st = service.status(id);
        return ResponseEntity.ok(new StatusResp(st.name()));
    }

    @Data
    public static class SendReq { private String email; }
    @Data
    public static class SendResp { private final String verificationId; }

    @Data
    public static class ConfirmReq { private String verificationId; private String code; }
    @Data
    public static class ConfirmResp { private final boolean verified; }

    @Data
    public static class StatusResp { private final String status; }
}