package com.erp.erp_back.entity.erp;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "industry_benchmark",
        uniqueConstraints = @UniqueConstraint(name = "uk_bench_scope",
                columnNames = {"sigungu_cd_nm", "industry", "sub_category_name"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IndustryBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sigungu_cd_nm", length = 50)
    private String sigunguCdNm; // nullable

    @Column(name = "industry", nullable = false, length = 30)
    private String industry;

    @Column(name = "sub_category_name", length = 30)
    private String subCategoryName; // nullable

    @Column(name = "avg_cogs_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal avgCogsRate;

    @Column(name = "avg_labor_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal avgLaborRate;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}