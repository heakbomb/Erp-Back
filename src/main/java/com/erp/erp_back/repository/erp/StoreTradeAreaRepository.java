package com.erp.erp_back.repository.erp;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.erp.erp_back.dto.erp.TradeAreaInfo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class StoreTradeAreaRepository {

    @PersistenceContext
    private EntityManager em;

    public TradeAreaInfo findTradeAreaInfo(Long storeId) {
        String sql = """
            SELECT trdar_cd, trdar_cd_nm, sigungu_cd_nm, distance_m, match_method
            FROM store_trade_area
            WHERE store_id = :storeId
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .getResultList();

        if (rows.isEmpty()) return null;

        Object[] r = rows.get(0);

        String trdarCd = r[0] != null ? String.valueOf(r[0]) : null;
        String trdarCdNm = r[1] != null ? String.valueOf(r[1]) : null;
        String sigunguCdNm = r[2] != null ? String.valueOf(r[2]) : null;

        Integer distanceM = null;
        if (r[3] != null) {
            if (r[3] instanceof Number n) distanceM = n.intValue();
            else distanceM = Integer.parseInt(String.valueOf(r[3]));
        }

        String matchMethod = r[4] != null ? String.valueOf(r[4]) : null;

        return new TradeAreaInfo(trdarCd, trdarCdNm, sigunguCdNm, distanceM, matchMethod);
    }
}
