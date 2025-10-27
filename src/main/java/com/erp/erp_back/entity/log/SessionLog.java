package com.erp.erp_back.entity.log;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.user.Owner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SessionLog")
@Data
@NoArgsConstructor
public class SessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "refresh_token", nullable = false, length = 255)
    private String refreshToken;
}