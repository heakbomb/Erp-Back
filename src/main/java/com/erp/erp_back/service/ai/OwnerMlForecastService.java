package com.erp.erp_back.service.ai;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.client.MlPredictClient;
import com.erp.erp_back.dto.ai.ProfitForecastResponse;
import com.erp.erp_back.entity.ai.MlProfitForecast;
import com.erp.erp_back.repository.ai.MlProfitForecastRepository;

// ✅ 프로젝트에 있는 예외/코드로 맞춰 써줘
import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class OwnerMlForecastService {

    private static final String TARGET = "y_next_profit";

    private final MlPredictClient mlPredictClient;
    private final MlProfitForecastRepository forecastRepo;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public ProfitForecastResponse getOrCreateProfitForecast(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String featureYm = ym.toString(); // "2025-12"
        // 보통 다음달 예측이면 predForYm = ym.plusMonths(1)

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

        // 2) ✅ feature row 존재 여부 선 체크 (없으면 FastAPI 호출하지 않음)
        if (!existsFeatureRow(storeId, featureYm)) {
            // 여기서 500이 아니라 “예측 불가(데이터 부족)”로 떨어져야 함
            throw new BusinessException(
                    ErrorCodes.ML_FEATURE_NOT_READY,
                    "예측에 필요한 데이터가 없습니다. (storeId=" + storeId + ", ym=" + featureYm + ")"
            );
        }

        // 3) 없으면 FastAPI 호출 (404/422 등을 의미 있게 변환)
        ProfitForecastResponse predicted;
        try {
            predicted = mlPredictClient.predictProfit(storeId, year, month);
        } catch (WebClientResponseException e) {
            // ✅ FastAPI가 404(특징 없음) 주는 경우를 500으로 만들지 말고 “데이터 부족”으로 변환
            if (e.getStatusCode().value() == 404) {
                throw new BusinessException(
                        ErrorCodes.ML_FEATURE_NOT_READY,
                        "예측 데이터가 없습니다. (storeId=" + storeId + ", ym=" + featureYm + ")"
                );
            }
            // ✅ 요청 파라미터 문제(400/422 등)
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 422) {
                throw new BusinessException(
                        ErrorCodes.ML_PREDICT_BAD_REQUEST,
                        "예측 요청이 유효하지 않습니다. (storeId=" + storeId + ", year=" + year + ", month=" + month + ")"
                );
            }
            // 그 외는 그대로 “서버 오류”로 올려도 되지만 message는 남기자
            throw new BusinessException(
                    ErrorCodes.ML_PREDICT_FAILED,
                    "예측 서버 호출 실패: " + e.getStatusCode()
            );
        } catch (Exception e) {
            // ✅ 네트워크/파싱/타임아웃 등
            throw new BusinessException(
                    ErrorCodes.ML_PREDICT_FAILED,
                    "예측 처리 중 오류: " + e.getMessage()
            );
        }

        // 4) 저장 (unique key로 upsert 느낌)
        MlProfitForecast entity = MlProfitForecast.builder()
                .storeId(predicted.storeId())
                .featureYm(predicted.featureYm())
                .predForYm(predicted.predForYm())
                .target(predicted.target())
                .predValue(predicted.pred())
                .modelPath(predicted.modelPath())
                .build();

        forecastRepo.save(entity);

        return predicted;
    }

    private boolean existsFeatureRow(Long storeId, String ym) {
        // ml_store_month_features 테이블에 row가 있는지 확인
        // (테이블/컬럼명은 네가 만든 그대로라 가정)
        String sql = """
            SELECT 1
            FROM ml_store_month_features
            WHERE store_id = :storeId
              AND ym = :ym
            LIMIT 1
        """;

        var result = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("ym", ym)
                .getResultList();

        return !result.isEmpty();
    }
}
