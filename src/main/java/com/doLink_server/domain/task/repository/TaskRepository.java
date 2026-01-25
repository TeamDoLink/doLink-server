package com.doLink_server.domain.task.repository;

import com.doLink_server.domain.task.entity.Task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 모음별 Task 전체 조회
     */
    List<Task> findAllByCollection_CollectionIdOrderByTaskIdDesc(Long collectionId);

    /**
     * 여러 모음에 대해 모음별 대표 썸네일 최대 4개를 한 번에 조회
     * - 최신 Task(created_at DESC) 기준
     * - thumbnail_key 있는 Task만 대상으로 함
     */
    @Query(value = """
        SELECT collection_id AS collectionId, thumbnail_key AS thumbnailKey
        FROM (
            SELECT
                t.collection_id,
                t.thumbnail_key,
                ROW_NUMBER() OVER (
                    PARTITION BY t.collection_id
                    ORDER BY t.created_at DESC
                ) rn
            FROM dolink.task t
            WHERE t.collection_id IN (:collectionIds)
              AND t.thumbnail_key IS NOT NULL
        ) x
        WHERE rn <= 4
        """, nativeQuery = true)
    List<CollectionThumbnailRow> findTop4ThumbnailsByCollectionIds(
            @Param("collectionIds") List<Long> collectionIds
    );
}
