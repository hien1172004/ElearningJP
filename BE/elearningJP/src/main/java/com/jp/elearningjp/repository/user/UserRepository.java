package com.jp.elearningjp.repository.user;

import com.jp.elearningjp.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String email);

    boolean existsByEmail(String email);

    /** Batch fetch user displayName + avatarUrl theo danh sách ID. */
    @Query("SELECT u.id, u.fullName, u.avatarUrl FROM User u WHERE u.id IN :ids")
    List<Object[]> findDisplayInfosByIds(Collection<Long> ids);
}