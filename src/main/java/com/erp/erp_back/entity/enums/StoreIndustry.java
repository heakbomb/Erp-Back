package com.erp.erp_back.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreIndustry {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    ASIAN("아시아 음식"),
    SNACK("분식"),
    BBQ("고기/구이"),
    SEAFOOD("해산물/회"),
    FAST_FOOD("패스트푸드"),
    CHICKEN("치킨"),
    CAFE("카페/디저트"),
    BAKERY("베이커리/제과점"),
    BAR("주점/술집"),
    FUSION("퓨전/기타");

    private final String description;
}