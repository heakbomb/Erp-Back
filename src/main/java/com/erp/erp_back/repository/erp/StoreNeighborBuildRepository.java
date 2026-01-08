package com.erp.erp_back.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StoreNeighborBuildRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public int upsertNeighborsForStore(Long storeId, int radiusM) {
        String sql = """
            INSERT INTO store_neighbor (store_id, neighbor_store_id, radius_m, distance_m, created_at, updated_at)
            SELECT
                s1.store_id AS store_id,
                s2.store_id AS neighbor_store_id,
                :radiusM AS radius_m,
                CAST((
                    6371000 * 2 * ASIN(SQRT(
                        POW(SIN(RADIANS((g2.latitude - g1.latitude)/2)), 2) +
                        COS(RADIANS(g1.latitude)) * COS(RADIANS(g2.latitude)) *
                        POW(SIN(RADIANS((g2.longitude - g1.longitude)/2)), 2)
                    ))
                ) AS SIGNED) AS distance_m,
                NOW() AS created_at,
                NOW() AS updated_at
            FROM store s1
            JOIN store_gps g1 ON g1.store_id = s1.store_id
            JOIN store s2 ON s2.industry = s1.industry AND s2.store_id <> s1.store_id
            JOIN store_gps g2 ON g2.store_id = s2.store_id
            WHERE s1.store_id = :storeId
            HAVING distance_m <= :radiusM
            ON DUPLICATE KEY UPDATE
                distance_m = VALUES(distance_m),
                updated_at = NOW()
        """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("storeId", storeId);
        q.setParameter("radiusM", radiusM);
        return q.executeUpdate();
    }

    @Transactional
    public int deleteNeighborsForStore(Long storeId, int radiusM) {
        String sql = """
            DELETE FROM store_neighbor
            WHERE store_id = :storeId AND radius_m = :radiusM
        """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("storeId", storeId);
        q.setParameter("radiusM", radiusM);
        return q.executeUpdate();
    }
}
