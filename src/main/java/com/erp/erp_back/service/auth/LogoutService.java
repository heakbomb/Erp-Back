package com.erp.erp_back.service.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.repository.auth.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void logout(Long ownerId) {
        refreshTokenRepository.revokeAllByOwnerId(ownerId);
    }
}