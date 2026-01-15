package com.erp.erp_back.repository.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.entity.auth.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.revoked = true
         where rt.owner.ownerId = :ownerId
           and rt.revoked = false
    """)
    int revokeAllByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
        select rt from RefreshToken rt
         where rt.token = :token
           and rt.revoked = false
           and rt.expiresAt > :now
    """)
    Optional<RefreshToken> findValid(@Param("token") String token, @Param("now") LocalDateTime now);
}