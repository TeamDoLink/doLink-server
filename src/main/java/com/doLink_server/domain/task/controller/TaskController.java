package com.doLink_server.domain.task.controller;

import com.doLink_server.domain.task.dto.TaskCreateRequest;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.service.TaskService;
import com.doLink_server.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
@Tag(name = "Task", description = "할 일 관련 API")
public class TaskController {

    private final TaskService taskService;

    /**
     * 할 일 추가
     */
    @PostMapping
    @Operation(summary = "할 일 추가", description = "할 일을 생성한다.")
    public ApiResponse<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.onSuccess(taskService.taskCreate(request));
    }

    /**
     * 모음별 Task 전체 조회, 페이지네이션으로 마이그레이션 필요!!
     */
//    @GetMapping("/collections/{collectionId}")
//    @Operation(summary = "모음별 Task 전체 조회", description = "collectionId에 속한 Task를 전부 조회한다.")
//    public ApiResponse<List<TaskResponse>> listByCollection(@PathVariable Long collectionId) {
//        return ApiResponse.onSuccess(taskService.listByCollection(collectionId));
//    }

    @GetMapping("/collections/{collectionId}")
    @Operation(summary = "모음별 Task 전체 조회", description = "collectionId에 속한 Task를 페이징(무한 스크롤)하여 조회한다.")
    public ApiResponse<Slice<TaskResponse>> listByCollection(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(taskService.listByCollection(collectionId, page, size));
    }

    /**
     * 할 일 단건 조회
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "할 일 상세 조회", description = "taskId에 해당하는 할 일을 조회한다.")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.onSuccess(taskService.getTask(taskId));
    }
}
