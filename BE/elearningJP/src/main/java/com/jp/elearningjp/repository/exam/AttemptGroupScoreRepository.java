package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.AttemptGroupScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptGroupScoreRepository extends JpaRepository<AttemptGroupScore, Long> {

    @Query("SELECT ags FROM AttemptGroupScore ags " +
           "JOIN FETCH ags.scoreGroup " +
           "WHERE ags.attempt.id = :attemptId AND ags.deleted = false")
    List<AttemptGroupScore> findByAttemptIdWithScoreGroup(@Param("attemptId") Long attemptId);
}
