package com.doLink_server.domain.collection.repository;

import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.global.enums.Category;
import com.doLink_server.user.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 모음 레포지토리
 */
public interface CollectionRepository extends JpaRepository<Collection, Long> {

    /**
     * 로그인한 사용자의 모음 전체 조회 (무한 스크롤)
     * - 생성일자 최신 순
     */
    Slice<Collection> findAllByUserOrderByCreatedAtDesc(
            Users user,
            Pageable pageable
    );

    /**
     * 로그인한 사용자의 카테고리별 모음 조회 (무한 스크롤)
     * - 생성일자 최신 순
     */
    Slice<Collection> findAllByUserAndCategoryOrderByCreatedAtDesc(
            Users user,
            Category category,
            Pageable pageable
    );

    /**
     * 사용자별 모음 목록 조회
     *
     * Users (1) : Collection (N)
     * - Collection.user -> Users
     * - Users.userId(byte[]) 기준으로 조회
     */
    List<Collection> findAllByUser_UserId(byte[] userId);

    /**
     * 모음 단건 조회 (User까지 fetch join)
     * - 삭제/수정 등 권한 체크 시 Lazy 로딩 추가 쿼리를 방지하기 위함
     */
    @Query("""
        select c
        from Collection c
        join fetch c.user u
        where c.collectionId = :collectionId
    """)
    Optional<Collection> findByIdWithUser(Long collectionId);

    /**
     * 모음 선택 UI용
     * - 로그인 사용자의 모음 전체 조회
     */
    List<Collection> findAllByUser(Users user);


}
