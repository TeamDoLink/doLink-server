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
}
