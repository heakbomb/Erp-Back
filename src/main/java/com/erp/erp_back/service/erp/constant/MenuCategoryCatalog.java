package com.erp.erp_back.service.erp.constant;

import java.util.*;

public final class MenuCategoryCatalog {

    private MenuCategoryCatalog() {}

    // 업종(대분류) 2개 고정
    public enum Industry {
        KOREAN, CHICKEN
    }

    /**
     * 규칙:
     * - key: Industry
     * - value: (중분류 -> 소분류 Set)
     */
    private static final Map<Industry, Map<String, Set<String>>> CATALOG;

    static {
        Map<Industry, Map<String, Set<String>>> root = new EnumMap<>(Industry.class);

        // ===== 한식 =====
        Map<String, Set<String>> korean = new LinkedHashMap<>();
        korean.put("면/국수", setOf("물냉면", "비빔냉면", "열무국수", "잔치국수", "비빔국수", "칼국수", "수제비", "콩국수", "막국수"));
        korean.put("국/탕", setOf("설렁탕", "곰탕", "갈비탕", "육개장", "삼계탕", "순대국", "뼈해장국"));
        korean.put("찌개/전골", setOf("김치찌개", "된장찌개", "순두부찌개", "부대찌개", "청국장", "버섯전골"));
        korean.put("밥/정식", setOf("백반", "비빔밥", "돌솥비빔밥", "제육정식", "불고기정식", "생선구이정식"));
        korean.put("찜/조림", setOf("갈비찜", "닭볶음탕", "김치찜", "고등어조림", "갈치조림", "코다리찜"));
        korean.put("전/부침", setOf("파전", "해물파전", "김치전", "감자전", "부추전", "모둠전"));
        korean.put("분식/간편식", setOf("떡볶이", "순대", "김밥", "만두", "튀김"));
        korean.put("사이드", setOf("공기밥", "사리추가", "계란말이", "잡채", "두부김치"));
        root.put(Industry.KOREAN, Collections.unmodifiableMap(korean));

        // ===== 치킨 =====
        Map<String, Set<String>> chicken = new LinkedHashMap<>();
        chicken.put("후라이드", setOf("후라이드", "크리스피", "옛날통닭", "반반치킨", "순살후라이드", "뼈후라이드"));
        chicken.put("양념/간장", setOf("양념치킨", "간장치킨", "마늘간장", "데리야끼", "허니버터", "간장마요"));
        chicken.put("매운치킨", setOf("매운양념", "불닭치킨", "고추치킨", "청양치킨", "마라치킨"));
        chicken.put("파/토핑", setOf("파닭", "양파치킨", "치즈치킨", "콘마요치킨", "갈릭치킨", "시즈닝치킨"));
        chicken.put("오븐/구이", setOf("오븐구이", "바비큐", "숯불치킨", "로스트치킨"));
        chicken.put("부분육", setOf("윙", "봉", "윙봉세트", "닭다리"));
        chicken.put("세트", setOf("치킨+감튀", "치킨+떡볶이", "1인세트", "가족세트"));
        chicken.put("사이드", setOf("감자튀김", "치즈볼", "치킨무", "치즈스틱", "오징어링"));
        root.put(Industry.CHICKEN, Collections.unmodifiableMap(chicken));

        CATALOG = Collections.unmodifiableMap(root);
    }

    public static Map<String, Set<String>> categoriesOf(Industry industry) {
        return CATALOG.getOrDefault(industry, Map.of());
    }

    /** ✅ 저장/수정 시 검증용 */
    public static boolean isValid(Industry industry, String categoryName, String subCategoryName) {
        if (industry == null || categoryName == null || subCategoryName == null) return false;
        Map<String, Set<String>> categoryMap = CATALOG.get(industry);
        if (categoryMap == null) return false;
        Set<String> subs = categoryMap.get(categoryName);
        return subs != null && subs.contains(subCategoryName);
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }
}