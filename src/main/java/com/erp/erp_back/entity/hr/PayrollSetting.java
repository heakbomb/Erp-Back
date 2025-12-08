package com.erp.erp_back.entity.hr;

import java.math.BigDecimal;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

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
@Table(name = "payroll_setting")
@Data
@NoArgsConstructor
public class PayrollSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "wage_type", nullable = false, length = 20)
    private String wageType;

    @Column(name = "base_wage", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseWage;

    @Column(name = "deduction_items", columnDefinition = "json")
    private String deductionItems;
}