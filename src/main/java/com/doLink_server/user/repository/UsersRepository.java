package com.doLink_server.user.repository;



import com.doLink_server.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 레포지토리
 */
@Repository
public interface UsersRepository extends JpaRepository<Users, UUID> {
    // socialId로 사용자 조회
    Optional<Users> findBySocialId(String socialId);
    // userId 사용자 조회
    Optional<Users> findByUserId(byte[] userId);

    Optional<Users> findBySocialIdAndSocialName(String socialId, String socialName);
}