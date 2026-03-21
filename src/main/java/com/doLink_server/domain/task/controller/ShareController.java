package com.doLink_server.domain.task.controller;

import com.doLink_server.domain.task.dto.ShareTokenResponse;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.service.TaskService;
import com.doLink_server.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Share", description = "할 일 공유 API")
public class ShareController {

    private final TaskService taskService;

    @PostMapping("/api/v1/task/{taskId}/share")
    @Operation(summary = "공유 토큰 발급", description = "할 일의 공유 토큰을 발급한다. (로그인 필요)")
    public ApiResponse<ShareTokenResponse> createShareToken(@PathVariable Long taskId) {
        String token = taskService.createShareToken(taskId);
        return ApiResponse.onSuccess(ShareTokenResponse.builder().shareToken(token).build());
    }

    @GetMapping("/api/v1/share/task/{shareToken}")
    @Operation(summary = "공유된 할 일 조회", description = "공유 토큰으로 할 일을 조회한다. (인증 불필요)")
    public ApiResponse<TaskResponse> getSharedTask(@PathVariable String shareToken) {
        return ApiResponse.onSuccess(taskService.getSharedTask(shareToken));
    }
}
