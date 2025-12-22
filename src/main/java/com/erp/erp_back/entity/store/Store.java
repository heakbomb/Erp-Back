package com.erp.erp_back.entity.store;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.enums.StoreIndustry; // ✅ Enum Import 필수!

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType; // 추가
import jakarta.persistence.Enumerated; // 추가
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
@Table(name = "store")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biz_id", nullable = false)
    private BusinessNumber businessNumber;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;
    
    // ✅ [수정 후 - 정답]
    @Enumerated(EnumType.STRING)     // DB에는 'KOREAN' 같은 문자열로 저장하겠다는 뜻
    @Column(nullable = false, length = 50)
    private StoreIndustry industry;  // 변수 타입도 반드시 Enum 클래스여야 함

    @Column(name = "pos_vendor", length = 50)
    private String posVendor;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}