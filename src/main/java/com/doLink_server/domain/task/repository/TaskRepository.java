package com.doLink_server.domain.task.repository;

import com.doLink_server.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 모음별 Task 전체 조회
     */
    List<Task> findAllByCollection_CollectionIdOrderByTaskIdDesc(Long collectionId);

}
