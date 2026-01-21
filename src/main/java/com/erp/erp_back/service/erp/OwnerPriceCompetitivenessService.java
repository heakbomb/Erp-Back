package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.PriceCompetitivenessItem;
import com.erp.erp_back.dto.erp.PriceCompetitivenessResponse;
import com.erp.erp_back.dto.erp.TradeAreaInfo;
import com.erp.erp_back.repository.erp.PriceCompetitivenessRepository;
import com.erp.erp_back.repository.erp.StoreNeighborRepository;
import com.erp.erp_back.repository.erp.StoreTradeAreaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerPriceCompetitivenessService {

    private final StoreNeighborRepository neighborRepo;
    private final StoreTradeAreaRepository tradeAreaRepo; // EntityManager 버전(클래스)
    private final PriceCompetitivenessRepository repo;

    /**
     * verdict 기준(너무 복잡하게 안감):
     * - 주변 평균가가 없으면 NO_DATA
     * - 내 가격이 평균보다 5% 이상 싸면 CHEAP
     * - 5% 이내면 FAIR
     * - 평균보다 5% 이상 비싸면 EXPENSIVE
     */
    public PriceCompetitivenessResponse analyze(Long storeId, int radiusM, boolean onlyActive) {
        int r = Math.max(100, Math.min(radiusM, 10000));

        // (1) 상권 라벨
        TradeAreaInfo tradeArea = tradeAreaRepo.findTradeAreaInfo(storeId);

        // (2) 이웃 총 수(매출/메뉴유무 무관)
        int nearTotal = neighborRepo.findByIdStoreIdAndIdRadiusM(storeId, r).size();

        // (3) 가격 비교 rows
        List<Object[]> rows = repo.fetchNeighborAvgBySameMenuName(storeId, r, onlyActive);

        List<PriceCompetitivenessItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            Long menuId = row[0] == null ? null : ((Number) row[0]).longValue();
            String menuName = row[1] == null ? null : String.valueOf(row[1]);

            BigDecimal myPrice = toBd(row[2]);
            BigDecimal neighborAvg = toBd(row[3]);
            Integer neighborStoreCount = toInt(row[4]);

            // diff
            BigDecimal diffPrice = null;
            BigDecimal diffRatePct = null;
            String verdict = "NO_DATA";

            if (neighborAvg != null && neighborAvg.compareTo(BigDecimal.ZERO) > 0) {
                diffPrice = myPrice.subtract(neighborAvg);

                diffRatePct = diffPrice
                        .divide(neighborAvg, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal absPct = diffRatePct.abs();
                if (absPct.compareTo(BigDecimal.valueOf(5)) < 0) verdict = "FAIR";
                else verdict = diffRatePct.compareTo(BigDecimal.ZERO) < 0 ? "CHEAP" : "EXPENSIVE";
            }

            items.add(new PriceCompetitivenessItem(
                    menuId,
                    menuName,
                    myPrice,
                    neighborAvg,
                    neighborStoreCount == null ? 0 : neighborStoreCount,
                    diffPrice,
                    diffRatePct,
                    verdict
            ));
        }

        return new PriceCompetitivenessResponse(storeId, r, nearTotal, tradeArea, items);
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO; // myPrice는 null이면 0 처리
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(v));
    }

    private Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(v));
    }
}
