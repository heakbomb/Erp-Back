package com.erp.erp_back.controller.erp;

import com.erp.erp_back.service.erp.constant.MenuCategoryCatalog;
import com.erp.erp_back.service.erp.constant.MenuCategoryCatalog.Industry;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/owner/menu/categories")
@CrossOrigin(origins = "http://localhost:3000")
public class MenuCategoryController {

    /**
     * ✅ 업종별 중분류 조회
     * GET /owner/menu/categories?industry=KOREAN
     */
    @GetMapping
    public List<String> getCategories(@RequestParam String industry) {
        Industry ind = normalizeIndustry(industry);
        return new ArrayList<>(MenuCategoryCatalog.categoriesOf(ind).keySet());
    }

    /**
     * ✅ 업종 + 중분류 → 소분류 조회
     * GET /owner/menu/categories/sub?industry=KOREAN&categoryName=면/국수
     */
    @GetMapping("/sub")
    public List<String> getSubCategories(
            @RequestParam String industry,
            @RequestParam String categoryName
    ) {
        Industry ind = normalizeIndustry(industry);

        Map<String, Set<String>> map = MenuCategoryCatalog.categoriesOf(ind);
        Set<String> subs = map.getOrDefault(categoryName, Collections.emptySet());
        return new ArrayList<>(subs);
    }

    /**
     * ✅ [추가] 업종 파라미터 정규화
     * - 프론트/스토어 값이 KOREAN, korean, KOREAN_FOOD 등으로 와도 KOREAN으로 매핑
     * - 기존 로직(카탈로그 조회/응답 형태)은 그대로 유지
     */
    private Industry normalizeIndustry(String v) {
        if (v == null) throw new IllegalArgumentException("industry is required");

        String x = v.trim().toUpperCase(Locale.ROOT);

        // ✅ 확장 호환: 문자열에 포함된 키워드로 매핑
        if (x.contains("KOREAN")) return Industry.KOREAN;
        if (x.contains("CHICKEN")) return Industry.CHICKEN;

        // ✅ 정확히 enum 값으로 오는 케이스
        return Industry.valueOf(x);
    }
}