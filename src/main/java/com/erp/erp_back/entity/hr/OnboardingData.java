package com.erp.erp_back.entity.hr;
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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OnboardingData")
@Data
@NoArgsConstructor
public class OnboardingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_id")
    private Long onboardingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "raw_json_data", nullable = false, columnDefinition = "json")
    private String rawJsonData;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
}