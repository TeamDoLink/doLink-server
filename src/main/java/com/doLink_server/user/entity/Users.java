package com.doLink_server.user.entity;

import com.doLink_server.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 기본정보 테이블
 */
@Getter
@Setter
@Entity
@Builder
@Table(name = "users", schema = "dolink")
@NoArgsConstructor
@AllArgsConstructor
public class Users extends BaseEntity {

    /**
     * 사용자 ID (연관관계 X, 단방향 UUID 관리)
     */
    @Id
    @Column(name = "user_id", nullable = false, length = 16)
    private byte[] userId;


    @Column(name = "task_id", columnDefinition = "BINARY(16)")
    private UUID taskId;

    /**
     * 할 일 모음 ID
     */
    @Column(name = "collection_id", nullable = true)
    private Long collectionId;

    /**
     * 소셜 로그인 제공 ID
     */
    @Column(name = "social_id", nullable = false, length = 10)
    private String socialId;

    /**
     * 소셜 명
     */
    @Column(name = "social_name", nullable = false, length = 8)
    private String socialName;

    /**
     * 사용자 이메일주소
     */
    @Column(name = "email", nullable = false, length = 30)
    private String email;

    /**
     * 계정 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * 프로필 이미지 URL 경로
     */
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    /**
     * 사용자 닉네임
     */
    @Column(name = "nickname", nullable = false, length = 15)
    private String nickname;

    /**
     * 할 일 제목
     */
    @Column(name = "title")
    private String title;

    /**
     * 관련 링크
     */
    @Column(name = "link")
    private String link;

    /**
     * 메모
     */
    @Column(name = "memo")
    private String memo;

    /**
     * 완료 상태
     */
    @Column(name = "status")
    private Boolean status;

    /**
     * 내부 / 외부 구분
     */
    @Column(name = "in_out")
    private Boolean inout;

    /**
     * 생성일
     */
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일
     */
    @NotNull
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;


}
