package com.erp.erp_back.service.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.auth.AdminLoginRequest;
import com.erp.erp_back.dto.auth.OwnerLoginResponse;
import com.erp.erp_back.entity.user.Admin;
import com.erp.erp_back.exception.BusinessException;
import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.repository.user.AdminRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그용

@Slf4j // 로그 사용
@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminRepository adminRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public OwnerLoginResponse login(AdminLoginRequest request) {
        // 1. 입력값 확인
        System.out.println("================ [로그인 디버깅] ================");
        System.out.println("입력된 아이디: " + request.getEmail());
        System.out.println("입력된 비밀번호: " + request.getPassword());

        // 2. 관리자 조회
        Admin admin = adminRepository.findByUsername(request.getEmail())
                .orElseThrow(() -> {
                    System.out.println(">> 실패: DB에서 해당 아이디를 찾을 수 없음!");
                    return new BusinessException(ErrorCodes.USER_NOT_FOUND, "관리자 계정을 찾을 수 없습니다.");
                });

        // 3. DB 값 확인
        System.out.println("DB에 저장된 비밀번호: [" + admin.getPassword() + "]");
        
        // 4. 비밀번호 검증 (단순 비교)
        if (!admin.getPassword().equals(request.getPassword())) {
            System.out.println(">> 실패: 비밀번호 불일치! (DB값 vs 입력값 다름)");
            System.out.println("   DB 길이: " + admin.getPassword().length());
            System.out.println("   입력 길이: " + request.getPassword().length());
            throw new BusinessException(ErrorCodes.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.");
        }

        System.out.println(">> 성공: 로그인 성공!");
        System.out.println("=============================================");

        // 5. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(admin.getAdminId(), admin.getUsername(), "ADMIN");
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getAdminId());

        return OwnerLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .ownerId(admin.getAdminId())
                .username(admin.getUsername())
                .build();
    }
}