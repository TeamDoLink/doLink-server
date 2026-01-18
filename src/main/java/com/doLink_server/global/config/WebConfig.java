package com.doLink_server.global.config;

import com.doLink_server.global.converter.StringToCategoryConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 전역 설정 클래스
 *
 * - Spring MVC에서 사용할 Converter / Formatter를 전역으로 등록
 * - Controller의 @RequestParam, @PathVariable 바인딩 시 자동 적용
 *
 * 현재 역할:
 * - String → Category enum 변환 Converter 등록
 *   (예: "FOOD", "food", "맛집" → Category.FOOD)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final StringToCategoryConverter stringToCategoryConverter;

    /**
     * Spring MVC에서 사용할 Formatter / Converter 등록
     *
     * - @RequestParam Category category
     * - @PathVariable Category category
     *
     * 와 같은 경우에 자동으로 StringToCategoryConverter가 적용됨
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToCategoryConverter);
    }
}
