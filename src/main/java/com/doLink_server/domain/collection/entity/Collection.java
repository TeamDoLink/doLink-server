package com.doLink_server.domain.collection.entity;


import com.doLink_server.global.common.BaseEntity;
import com.doLink_server.global.enums.Category;
import com.doLink_server.user.entity.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


/**
 * 모음 테이블
 */
@Getter
@Setter
@Entity
@Builder
@Table(name = "collection", schema = "dolink")
@NoArgsConstructor
@AllArgsConstructor
public class Collection extends BaseEntity {

    /**
     * 모음 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_id")
    private Long collectionId;

    /**
     * 모음 이름
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    /**
     * 모음 카테고리
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    /**
     * 튜토리얼 여부
     */
    @Column(name = "is_tutorial")
    private Boolean isTutorial;

    /**
     * 사용자 (Users 1 : Collection N)
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private Users user;
}
