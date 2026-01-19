package com.erp.erp_back.controller.auth;

import com.erp.erp_back.dto.auth.OwnerRegisterRequest;
import com.erp.erp_back.dto.auth.OwnerRegisterResponse;
import com.erp.erp_back.service.auth.OwnerRegisterService;
import com.erp.erp_back.repository.user.OwnerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/register")
public class OwnerAuthController {

    private final OwnerRegisterService ownerRegisterService;
    private final OwnerRepository ownerRepository; 

    @PostMapping("/owner")
    public ResponseEntity<OwnerRegisterResponse> registerOwner(@Valid @RequestBody OwnerRegisterRequest req) {
        Long ownerId = ownerRegisterService.register(req);
        return ResponseEntity.ok(new OwnerRegisterResponse(ownerId));
    }

     // ✅ 추가: 이메일 중복 체크 (회원가입 STEP1에서 사용)
    @GetMapping("/owner/exists")
    public ResponseEntity<OwnerExistsResponse> existsOwner(@RequestParam("email") String email) {
        String normalized = (email == null) ? "" : email.trim().toLowerCase();
        boolean exists = !normalized.isBlank() && ownerRepository.existsByEmail(normalized);
        return ResponseEntity.ok(new OwnerExistsResponse(exists));
    }

    public record OwnerExistsResponse(boolean exists) {}
}