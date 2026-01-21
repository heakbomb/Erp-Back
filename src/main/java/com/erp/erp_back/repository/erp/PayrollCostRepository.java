package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PayrollCostRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 월 인건비(세전) 합계
     */
    public BigDecimal sumMonthlyLabor(Long storeId, String payrollMonth) {
        String sql = """
            SELECT COALESCE(SUM(ph.gross_pay), 0)
            FROM payroll_history ph
            WHERE ph.store_id = :storeId
              AND ph.payroll_month = :payrollMonth
              AND ph.status = 'PENDING'
        """;

        Object v = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("payrollMonth", payrollMonth)
                .getSingleResult();

        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(v));
    }
}
