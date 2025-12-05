package com.erp.erp_back.entity.enums;

public enum IngredientCategory {
    VEGETABLE("농산물"),
    MEAT("축산물"),
    SEAFOOD("수산물"),
    SEASONING("조미료/양념"),
    GRAIN("곡류/전분"),
    PROCESSED_FOOD("면/가공식품"),
    CANNED("통조림/절임"),
    BAKERY("베이커리/파티쉐"),
    CAFE("카페 재료"),
    BEVERAGE_BASE("음료 베이스"),
    FROZEN("냉동/신선"),
    ETC("기타");

    private final String labelKo;

    IngredientCategory(String labelKo) {
        this.labelKo = labelKo;
    }

    public String getLabelKo() {
        return labelKo;
    }
}
