package com.erp.erp_back.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    // Corrected relationship: Store has a Many-to-One with BusinessNumber via biz_id 
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
}