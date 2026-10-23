package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {

}