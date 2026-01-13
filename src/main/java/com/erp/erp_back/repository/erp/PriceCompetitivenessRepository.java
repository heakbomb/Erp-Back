package com.erp.erp_back.repository.erp;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PriceCompetitivenessRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 내 메뉴(기준 storeId)의 각 menuName에 대해
     * 2km 동종업종 이웃 매장들의 동일 menuName 평균 price 계산
     *
     * 반환 컬럼:
     * 0 menu_id
     * 1 menu_name
     * 2 my_price
     * 3 neighbor_avg_price
     * 4 neighbor_store_count
     */
    public List<Object[]> fetchNeighborAvgBySameMenuName(Long storeId, int radiusM, boolean onlyActive) {
        String sql = """
            SELECT
                m.menu_id,
                m.menu_name,
                m.price AS my_price,
                agg.neighbor_avg_price,
                agg.neighbor_store_count
            FROM menu_item m
            LEFT JOIN (
                SELECT
                    mi.menu_name,
                    AVG(mi.price) AS neighbor_avg_price,
                    COUNT(DISTINCT mi.store_id) AS neighbor_store_count
                FROM store_neighbor sn
                JOIN menu_item mi
                    ON mi.store_id = sn.neighbor_store_id
                WHERE sn.store_id = :storeId
                  AND sn.radius_m = :radiusM
                  AND (:onlyActive = FALSE OR mi.status = 'ACTIVE')
                GROUP BY mi.menu_name
            ) agg ON agg.menu_name = m.menu_name
            WHERE m.store_id = :storeId
              AND (:onlyActive = FALSE OR m.status = 'ACTIVE')
            ORDER BY m.menu_name ASC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("radiusM", radiusM)
                .setParameter("onlyActive", onlyActive)
                .getResultList();

        return rows;
    }
}
