package com.doLink_server.domain.task.repository;

import com.doLink_server.domain.task.entity.Task;

import com.doLink_server.user.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    /**
     * 할 일 단건 조회 (User, Collection까지 fetch join)
     * - 삭제/수정 등 권한 체크 시 Lazy 로딩 추가 쿼리를 방지하기 위함
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.user u JOIN FETCH t.collection c WHERE t.taskId = :taskId")
    Optional<Task> findByIdWithUserAndCollection(@Param("taskId") Long taskId);

    /**
     * 할 일 단건 조회 (User까지 fetch join)
     * - 삭제/수정 등 권한 체크 시 Lazy 로딩 추가 쿼리를 방지하기 위함
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.user c WHERE t.taskId = :taskId")
    Optional<Task> findByIdWithUser(@Param("taskId") Long taskId);

    /**
     * 모음별 Task 전체 조회 (페이징, 무한 스크롤)
     */
    Slice<Task> findAllByCollection_CollectionId(Long collectionId, Pageable pageable);

    /**
     * 모음별 Task 전체 조회 (페이징, 무한 스크롤) + 완료/미완료 필터
     *
     * @param status true: 완료, false: 미완료
     */
    Slice<Task> findAllByCollection_CollectionIdAndStatus(Long collectionId, Boolean status, Pageable pageable);

    /**
     * [검색용] 사용자별 할 일 제목 포함 검색 (페이징)
     */
    Slice<Task> findByUserAndTitleContaining(Users user, String keyword, Pageable pageable);
}
