package com.erp.erp_back.service.erp;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.erp.erp_back.dto.erp.WeeklyAreaAvgPoint;
import com.erp.erp_back.dto.erp.WeeklyAreaAvgResponse;
import com.erp.erp_back.repository.erp.StoreNeighborRepository;
import com.erp.erp_back.repository.erp.WeeklyAreaAvgFromDailyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerWeeklyAreaAvgService {

    private final StoreNeighborRepository neighborRepo;
    private final WeeklyAreaAvgFromDailyRepository avgRepo;

    public WeeklyAreaAvgResponse getWeeklyAreaAvg(Long storeId, int year, int month, int radiusM) {
        int y = Math.max(2000, Math.min(year, 2100));
        int m = Math.max(1, Math.min(month, 12));
        int r = Math.max(100, Math.min(radiusM, 10000));

        // 1) 반경 내 이웃 매장 총 수(매출 유무 무관)
        int nearTotal = neighborRepo.findByIdStoreIdAndIdRadiusM(storeId, r).size();

        // 2) 해당 월 범위
        LocalDate from = LocalDate.of(y, m, 1);
        LocalDate to = from.plusMonths(1);

        // 3) 주차별 평균
        List<WeeklyAreaAvgFromDailyRepository.Row> rows =
                avgRepo.fetchWeeklyAreaAvg(storeId, r, from, to);

        Map<Integer, WeeklyAreaAvgPoint> map = new HashMap<>();
        for (var row : rows) {
            map.put(row.getWeekIndex(),
                    new WeeklyAreaAvgPoint(
                            row.getWeekIndex(),
                            row.getAreaAvgSales() == null ? BigDecimal.ZERO : row.getAreaAvgSales(),
                            row.getNearStoreCount() == null ? 0 : row.getNearStoreCount()
                    ));
        }

        // 그래프 안정: 1~6주차 고정
        List<WeeklyAreaAvgPoint> data = new ArrayList<>();
        for (int w = 1; w <= 6; w++) {
            data.add(map.getOrDefault(w, new WeeklyAreaAvgPoint(w, BigDecimal.ZERO, 0)));
        }

        return new WeeklyAreaAvgResponse(storeId, y, m, r, nearTotal, data);
    }
}
