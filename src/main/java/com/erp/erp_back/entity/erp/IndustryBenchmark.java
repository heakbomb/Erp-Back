// src/main/java/com/erp/erp_back/entity/erp/IndustryBenchmark.java
package com.erp.erp_back.entity.erp;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "industry_benchmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bench_scope_ym",
                columnNames = {"year_month", "sigungu_cd_nm", "industry", "sub_category_name"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IndustryBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "YYYY-MM" */
    @Column(name = "year_month", length = 7, nullable = false)
    private String yearMonth;

    /** 예: "강남구", "종로구" (NULL 허용: 업종 전체 평균도 저장 가능) */
    @Column(name = "sigungu_cd_nm", length = 50)
    private String sigunguCdNm;

    /** 예: "KOREAN", "CHICKEN" */
    @Column(name = "industry", nullable = false, length = 30)
    private String industry;

    /** 구×업종 평균이면 "ALL" 고정 추천 (NULL은 유니크가 애매해질 수 있음) */
    @Column(name = "sub_category_name", length = 100)
    private String subCategoryName;

    /** 0~1 (예: 0.3500) */
    @Column(name = "avg_cogs_rate", nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal avgCogsRate = BigDecimal.ZERO;

    /** 0~1 (예: 0.2580) */
    @Column(name = "avg_labor_rate", nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal avgLaborRate = BigDecimal.ZERO;

    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private Integer sampleCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.updatedAt = LocalDateTime.now();
        if (this.subCategoryName == null || this.subCategoryName.isBlank()) {
            this.subCategoryName = "ALL";
        }
    }
}
