package com.doLink_server.domain.task.controller;

import com.doLink_server.domain.task.dto.TaskCreateRequest;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.dto.TaskUpdateRequest;
import com.doLink_server.domain.task.service.TaskService;
import com.doLink_server.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

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
     * 모음별 Task 전체 조회 (페이징, 무한 스크롤)
     */
    @GetMapping("/collections/{collectionId}")
    @Operation(summary = "모음별 Task 전체 조회", description = "collectionId에 속한 Task를 페이징(무한 스크롤)하여 조회한다.")
    public ApiResponse<Slice<TaskResponse>> listByCollection(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        return ApiResponse.onSuccess(taskService.listByCollection(collectionId, page, size, sort));
    }

    /**
     * 할 일 단건 조회
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "할 일 상세 조회", description = "taskId에 해당하는 할 일을 조회한다.")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.onSuccess(taskService.getTask(taskId));
    }

    /**
     * 할 일 수정
     */
    @PatchMapping("/{taskId}")
    @Operation(summary = "할 일 수정", description = "할 일의 제목, 링크, 메모, 상태 등을 수정한다.")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request
    ) {
        return ApiResponse.onSuccess(taskService.updateTask(taskId, request));
    }

    /**
     * 할 일 완료 처리
     */
    @PatchMapping("/{taskId}/complete")
    @Operation(summary = "할 일 완료 처리", description = "할 일의 상태를 완료(true)로 변경한다.")
    public ApiResponse<TaskResponse> completeTask(@PathVariable Long taskId) {
        return ApiResponse.onSuccess(taskService.completeTask(taskId));
    }

    /**
     * 할 일 삭제
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "할 일 삭제", description = "taskId에 해당하는 할 일을 삭제한다.")
    public ApiResponse<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.onSuccess("할 일이 삭제되었습니다.");
    }
}
