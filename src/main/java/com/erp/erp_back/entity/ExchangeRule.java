package com.erp.erp_back.entity;

import java.math.BigDecimal;

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

@Entity
@Table(name = "ExchangeRule")
@Getter
@NoArgsConstructor
public class ExchangeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "base_unit", nullable = false, length = 10)
    private String baseUnit;

    @Column(name = "target_unit", nullable = false, length = 10)
    private String targetUnit;

    @Column(name = "factor", nullable = false, precision = 10, scale = 5)
    private BigDecimal factor;
}