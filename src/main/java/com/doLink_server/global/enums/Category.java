package com.doLink_server.global.enums;

import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum Category {

    FOOD("맛집"),
    HOBBY("취미"),
    TRAVEL("여행"),
    FINANCE("재테크"),
    SHOPPING("쇼핑"),
    WORKOUT("운동"),
    CAREER("커리어"),
    SELF_DEV("자기개발"),
    LIFE_HACK("꿀팁"),
    ETC("기타");

    private final String labelKorean;

    Category(String labelKorean) {
        this.labelKorean = labelKorean;
    }

    /** API 응답 시 내려보낼 한글 라벨 */
    @JsonValue
    public String getLabelKorean() {
        return labelKorean;
    }

    private static final Map<String, Category> lookup = new HashMap<>();

    static {
        for (Category c : values()) {
            lookup.put(c.name().toLowerCase(), c);   // "food"
            lookup.put(c.labelKorean, c);            // "맛집"
        }
    }

    /** 요청(JSON)으로 들어온 값(한글 or 영어) → Enum 변환 */
    @JsonCreator
    public static Category from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus._INVALID_CATEGORY);
        }

        Category c = lookup.get(value.trim().toLowerCase());
        if (c != null) return c;

        // 혹시 모를 한글 직접 비교 fallback
        String trimmed = value.trim();
        for (Category category : values()) {
            if (category.labelKorean.equalsIgnoreCase(trimmed)) return category;
        }

        throw new GeneralException(ErrorStatus._INVALID_CATEGORY);
    }

}
