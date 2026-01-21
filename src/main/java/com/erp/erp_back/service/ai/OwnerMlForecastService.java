package com.erp.erp_back.service.ai;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.client.MlPredictClient;
import com.erp.erp_back.dto.ai.ProfitForecastResponse;
import com.erp.erp_back.entity.ai.MlProfitForecast;
import com.erp.erp_back.repository.ai.MlProfitForecastRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerMlForecastService {

    private static final String TARGET = "y_next_profit";

    private final MlPredictClient mlPredictClient;
    private final MlProfitForecastRepository forecastRepo;

    @Transactional
    public ProfitForecastResponse getOrCreateProfitForecast(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String featureYm = ym.toString(); // "2025-12"

        // 1) DB 캐시 조회
        var cachedOpt = forecastRepo.findTopByStoreIdAndFeatureYmAndTarget(storeId, featureYm, TARGET);
        if (cachedOpt.isPresent()) {
            MlProfitForecast c = cachedOpt.get();
            return new ProfitForecastResponse(
                    c.getStoreId(),
                    year,
                    month,
                    c.getFeatureYm(),
                    c.getPredForYm(),
                    c.getTarget(),
                    c.getPredValue(),
                    c.getModelPath()
            );
        }

        // 2) 없으면 FastAPI 호출
        ProfitForecastResponse predicted = mlPredictClient.predictProfit(storeId, year, month);

        // 3) 저장(upsert 느낌으로: unique key가 있으니 save로 충분)
        MlProfitForecast entity = MlProfitForecast.builder()
                .storeId(predicted.storeId())
                .featureYm(predicted.featureYm())
                .predForYm(predicted.predForYm())
                .target(predicted.target())
                .predValue(predicted.pred())
                .modelPath(predicted.modelPath())
                .build();

        // 혹시 race condition 대비: unique 키 충돌 시에는 다시 조회로 처리해도 되지만
        // 지금은 단순 save로 충분(필요하면 try/catch 추가 가능)
        forecastRepo.save(entity);

        return predicted;
    }
}
