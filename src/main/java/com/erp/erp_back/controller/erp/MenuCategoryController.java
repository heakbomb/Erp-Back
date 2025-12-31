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
    public List<String> getCategories(
            @RequestParam Industry industry
    ) {
        return new ArrayList<>(
                MenuCategoryCatalog.categoriesOf(industry).keySet()
        );
    }

    /**
     * ✅ 업종 + 중분류 → 소분류 조회
     * GET /owner/menu/categories/sub?industry=KOREAN&categoryName=면/국수
     */
    @GetMapping("/sub")
    public List<String> getSubCategories(
            @RequestParam Industry industry,
            @RequestParam String categoryName
    ) {
        Map<String, Set<String>> map = MenuCategoryCatalog.categoriesOf(industry);
        Set<String> subs = map.getOrDefault(categoryName, Collections.emptySet());
        return new ArrayList<>(subs);
    }
}