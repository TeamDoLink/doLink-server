package com.doLink_server.global.converter;

import com.doLink_server.global.enums.Category;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * String → Category enum 변환 Converter
 *
 * 사용 목적:
 * - @RequestParam / @PathVariable 로 들어오는 문자열을
 *   Category enum으로 안전하게 변환하기 위함
 *
 * 지원 예시:
 * - "FOOD"
 * - "food"
 * - "맛집"
 *
 * → 모두 Category.FOOD 로 변환
 *
 * 적용 범위:
 * - Controller의 @RequestParam Category
 * - Controller의 @PathVariable Category
 */
@Component
public class StringToCategoryConverter implements Converter<String, Category> {


    /**
     * 문자열 값을 Category enum으로 변환
     *
     * @param source 요청 파라미터로 전달된 문자열
     * @return 변환된 Category enum
     */
    @Override
    public Category convert(String source) {
        return Category.from(source); // - 영어(enum name) / 한글(label) 모두 허용
    }

}
