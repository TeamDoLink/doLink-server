package com.doLink_server.domain.task.controller;

import com.doLink_server.domain.task.dto.LinkCreateResult;
import com.doLink_server.domain.task.service.LinkCreateService;
import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.infra.og.dto.LinkPreviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * (S3+OG 메타데이터) 임시 확인용 컨트롤러
 * prod 에서 삭제
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/link")
@Tag(name = "Link", description = "링크 미리보기(OG) API")
public class LinkController {

    private final LinkCreateService linkCreateService;

    /**
     * 링크 미리보기 (OG 파싱 + S3 업로드 테스트용)
     */
    @PostMapping("/preview")
    @Operation(summary = "링크 미리보기", description = "OG 파싱 및 썸네일 S3 업로드 결과를 반환한다.")
    public ApiResponse<LinkCreateResult> preview(
            @Valid @RequestBody LinkPreviewRequest request
    ) {
        return ApiResponse.onSuccess(
                linkCreateService.create(request.url())
        );
    }

}
