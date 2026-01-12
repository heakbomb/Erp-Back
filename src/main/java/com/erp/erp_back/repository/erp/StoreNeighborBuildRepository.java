package com.erp.erp_back.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class StoreNeighborBuildRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * storeId 기준으로 (동종 업종) 반경 radiusM 이내 이웃 매장들을 store_neighbor에 upsert
     *
     * 전제:
     * - store_neighbor에 UNIQUE(store_id, neighbor_store_id, radius_m) 존재해야 ON DUPLICATE KEY가 동작함
     * - store_gps는 store_id(1:1)로 존재해야 함
     */
    @Transactional
    public int upsertNeighborsForStore(Long storeId, int radiusM) {

        // ✅ HAVING 별칭 이슈를 피하려고 서브쿼리로 감싼 안전 버전
        String sql = """
            INSERT INTO store_neighbor (store_id, neighbor_store_id, radius_m, distance_m, created_at, updated_at)
            SELECT
                t.store_id,
                t.neighbor_store_id,
                t.radius_m,
                t.distance_m,
                t.created_at,
                t.updated_at
            FROM (
                SELECT
                    s1.store_id AS store_id,
                    s2.store_id AS neighbor_store_id,
                    :radiusM     AS radius_m,
                    CAST((
                        6371000 * 2 * ASIN(SQRT(
                            POW(SIN(RADIANS((g2.latitude - g1.latitude) / 2)), 2) +
                            COS(RADIANS(g1.latitude)) * COS(RADIANS(g2.latitude)) *
                            POW(SIN(RADIANS((g2.longitude - g1.longitude) / 2)), 2)
                        ))
                    ) AS SIGNED) AS distance_m,
                    NOW() AS created_at,
                    NOW() AS updated_at
                FROM store s1
                JOIN store_gps g1 ON g1.store_id = s1.store_id
                JOIN store s2 ON s2.industry = s1.industry AND s2.store_id <> s1.store_id
                JOIN store_gps g2 ON g2.store_id = s2.store_id
                WHERE s1.store_id = :storeId
            ) t
            WHERE t.distance_m <= :radiusM
            ON DUPLICATE KEY UPDATE
                distance_m = VALUES(distance_m),
                updated_at = NOW()
        """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("storeId", storeId);
        q.setParameter("radiusM", radiusM);
        return q.executeUpdate();
    }

    /**
     * storeId + radiusM 기준 이웃 목록 삭제
     */
    @Transactional
    public int deleteNeighborsForStore(Long storeId, int radiusM) {
        String sql = """
            DELETE FROM store_neighbor
            WHERE store_id = :storeId
              AND radius_m = :radiusM
        """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("storeId", storeId);
        q.setParameter("radiusM", radiusM);
        return q.executeUpdate();
    }

    /**
     * storeId 중심으로 radiusM 이내에 있는 "주변 매장 store_id 목록" 조회 (업종 무관)
     *
     * 목적:
     * - storeId가 생성/이동했을 때, 영향권에 있는 다른 매장들도 rebuild 해주기 위함
     *
     * 반환:
     * - 반경 내 store_id들 (본인 포함 가능)
     */
    @Transactional(readOnly = true)
    public List<Long> findStoreIdsWithinRadius(Long storeId, int radiusM) {

        String sql = """
            SELECT g2.store_id
            FROM store_gps g1
            JOIN store_gps g2 ON 1=1
            WHERE g1.store_id = :storeId
              AND (
                6371000 * 2 * ASIN(SQRT(
                    POW(SIN(RADIANS((g2.latitude - g1.latitude) / 2)), 2) +
                    COS(RADIANS(g1.latitude)) * COS(RADIANS(g2.latitude)) *
                    POW(SIN(RADIANS((g2.longitude - g1.longitude) / 2)), 2)
                ))
              ) <= :radiusM
        """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("storeId", storeId);
        q.setParameter("radiusM", radiusM);

        @SuppressWarnings("unchecked")
        List<Number> rows = q.getResultList();

        return rows.stream().map(Number::longValue).toList();
    }
}
