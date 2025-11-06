package com.erp.erp_back.entity.store;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.erp_back.entity.user.Owner;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "business_number")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biz_id")
    private Long bizId; // PK

    /** 사장(Owner)와 다대일 관계 — 한 명의 사장이 여러 사업자 번호를 가질 수 있음 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private Owner owner;

    /**  사업자 대표 연락처 */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 사업자등록번호 (하이픈 제거 10자리) */
    @Column(name = "biz_num", nullable = false, unique = true, length = 10)
    private String bizNum;

    /** 국세청 인증 정보 추가 필드들 */
    @Column(name = "open_status", length = 50)
    private String openStatus; // 계속사업자 / 폐업자 등

    @Column(name = "tax_type", length = 50)
    private String taxType; // 과세유형 (예: 부가가치세 일반과세자)

    @Column(name = "start_dt", length = 10)
    private String startDt; // 개업일자 (YYYYMMDD)

    @Column(name = "end_dt", length = 10)
    private String endDt; // 폐업일자 (YYYYMMDD)

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt; // 인증된 시각

    /**  한 사업자번호가 여러 사업장(Store)을 가질 수 있음 */
    @OneToMany(mappedBy = "businessNumber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Store> stores;
}
