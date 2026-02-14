package com.doLink_server.domain.task.entity;

import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.global.common.BaseEntity;
import com.doLink_server.user.entity.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 할 일(Task) 테이블
 */
@Getter
@Setter
@Entity
@Builder
@Table(name = "task", schema = "dolink")
@NoArgsConstructor
@AllArgsConstructor
public class Task extends BaseEntity {

    /**
     * 할 일 ID (AUTO INCREMENT)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    /**
     * 사용자 (Users 1 : Task N)
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private Users user;

    /**
     * 모음 (Collection 1 : Task N)
     * - 모음 삭제 시 할 일도 같이 삭제되길 원하면 CASCADE
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    /**
     * 제목
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 링크
     */
    @Column(name = "link")
    private String link;

    /**
     * OG 대표 썸네일 S3 key
     * - DB에는 URL이 아니라 key만 저장 (presigned는 조회 시 생성)
     */
    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    /**
     * OG 원본 이미지 S3 key
     * - DB에는 URL이 아니라 key만 저장
     */
    @Column(name = "og_image_key", length = 500)
    private String ogImageKey;


    /**
     * 메모
     */
    @Column(name = "memo")
    private String memo;

    /**
     * 상태 (완료 여부)
     */
    @Column(name = "status")
    private Boolean status;

    /**
     * 내부: true
     * 외부: false
     */
    @Column(name = "in_out")
    private Boolean inout;

    /**
     * 튜토리얼 여부
     */
    @Column(name = "is_tutorial")
    private Boolean isTutorial;

    /**
     * 기본 정보 수정 메서드
     * - 값이 null이 아닐 때만 변경 (PATCH 방식)
     */
    public void updateElements(String title, String memo) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }

    /**
     * 링크 및 OG 데이터 수정 메서드
     * - 링크 관련 정보는 세트로 움직이므로 별도 메서드로 분리
     */
    public void updateLink(String link, String ogImageKey, String thumbnailKey) {
        this.link = link;
        this.ogImageKey = ogImageKey;
        this.thumbnailKey = thumbnailKey;
    }

    /**
     * 완료 상태 토글 메서드
     * - 완료(true) -> 미완료(false)
     * - 미완료(false) -> 완료(true)
     */
    public void toggleStatus() {
        this.status = !Boolean.TRUE.equals(this.status);
    }
}
