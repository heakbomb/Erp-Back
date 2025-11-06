package com.erp.erp_back.entity.log;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "attendance_qr_token")
@Getter @Setter
@NoArgsConstructor
public class AttendanceQrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "token_value", nullable = false, unique = true, length = 200)
    private String tokenValue;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;
}