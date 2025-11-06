package com.erp.erp_back.entity.store;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.enums.CostingMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Store") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    // ✅ 사업자 번호 (BusinessNumber 테이블과 N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biz_id", nullable = false)
    private BusinessNumber businessNumber;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, length = 50)
    private String industry;

    @Column(name = "pos_vendor", length = 50)
    private String posVendor;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ✅ 허용된 IP/CIDR 목록 (이전 버전용 — 현재는 QR/GPS 기반이지만 컬럼은 유지)
    @Column(name = "allowed_cidr_list", length = 512)
    private String allowedCidrList;

    // ✅ 매장 GPS 좌표 (QR + GPS 기반 출퇴근 검증용)
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // ✅ 허용 반경 (미터 단위)
    @Column(name = "gps_radius_m")
    private Integer gpsRadiusM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostingMethod costingMethod = CostingMethod.WEIGHTED_AVERAGE;

}