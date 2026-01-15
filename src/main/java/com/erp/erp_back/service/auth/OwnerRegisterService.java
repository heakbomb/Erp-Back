package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.auth.OwnerRegisterRequest;
import com.erp.erp_back.entity.enums.VerificationStatus;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerRegisterService {

    private final OwnerRepository ownerRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(OwnerRegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("요청 값이 비어있습니다.");
        }

        final String username = req.getUsername() == null ? "" : req.getUsername().trim();
        final String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        final String password = req.getPassword() == null ? "" : req.getPassword();
        final String confirmPassword = req.getConfirmPassword() == null ? "" : req.getConfirmPassword();
        final String verificationId = req.getVerificationId() == null ? "" : req.getVerificationId().trim();

        // 0) 필수값
        if (username.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        if (email.isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (verificationId.isEmpty()) {
            throw new IllegalArgumentException("이메일 인증 정보가 없습니다. 인증을 다시 진행해주세요.");
        }
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 1) 비번 확인
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 2) 이메일 인증 상태 확인
        VerificationStatus st = emailVerificationService.status(verificationId);
        if (st != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 3) verificationId에 묶인 이메일과 요청 이메일 일치 검증
        // (보안상 필수: 다른 사람이 인증한 verificationId를 탈취해도 이메일이 다르면 가입 불가)
        String verifiedEmail = emailVerificationService.getEmail(verificationId);
        if (verifiedEmail == null || verifiedEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("인증 정보를 확인할 수 없습니다. 다시 인증해주세요.");
        }
        if (!email.equals(verifiedEmail.trim().toLowerCase())) {
            throw new IllegalArgumentException("인증된 이메일과 요청 이메일이 일치하지 않습니다.");
        }

        // 4) 중복 체크
        if (ownerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 5) 저장
        Owner owner = Owner.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .salt("bcrypt") // 기존 컬럼 유지용(의미 없음). 가능하면 추후 컬럼 제거 권장
                .createdAt(LocalDateTime.now())
                .build();

        Owner saved = ownerRepository.save(owner);

        // 6) verificationId 재사용 방지 처리
        // (consume이 delete든 USED 처리든, 정책에 맞게 EmailVerificationService 내부에서 처리)
        emailVerificationService.consume(verificationId);

        return saved.getOwnerId();
    }
}