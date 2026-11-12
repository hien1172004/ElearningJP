package com.jp.elearningjp.repository.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jp.elearningjp.entity.curriculum.LessonAutosave;

import java.time.Instant;
import java.util.List;

@Repository
public interface LessonAutosaveRepository extends JpaRepository<LessonAutosave, Long> {

    /** Bản chưa lưu của 1 người trong 1 bài (mới nhất trước). */
    List<LessonAutosave> findByUserIdAndLessonIdOrderByUpdatedAtDesc(Long userId, Long lessonId);

    long countByUserIdAndLessonId(Long userId, Long lessonId);

    /**
     * Ghi đè bản chưa lưu (upsert theo user + bài + form). Câu lệnh native nên không tăng version
     * nội dung bài và không đụng tới khóa / trạng thái duyệt của bài.
     */
    @Modifying
    @Query(value = """
            INSERT INTO lesson_autosaves (user_id, lesson_id, form_key, content, updated_at)
            VALUES (:userId, :lessonId, :formKey, CAST(:content AS jsonb), :at)
            ON CONFLICT (user_id, lesson_id, form_key)
            DO UPDATE SET content = EXCLUDED.content, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("lessonId") Long lessonId, @Param("formKey") String formKey,
               @Param("content") String content, @Param("at") Instant at);

    @Modifying
    @Query("DELETE FROM LessonAutosave a WHERE a.userId = :userId AND a.lessonId = :lessonId AND a.formKey = :formKey")
    int deleteOne(@Param("userId") Long userId, @Param("lessonId") Long lessonId, @Param("formKey") String formKey);

    /** Người còn bản chưa lưu của các bài này (báo DRAFT_STALE khi bản live đổi). */
    @Query("SELECT DISTINCT a.userId FROM LessonAutosave a WHERE a.lessonId IN :lessonIds")
    List<Long> findUserIdsByLessonIds(@Param("lessonIds") java.util.Collection<Long> lessonIds);

    /** Người còn bản chưa lưu của bài bất kỳ trong khóa (lưu / duyệt cả khóa). */
    @Query(value = "SELECT DISTINCT a.user_id FROM lesson_autosaves a JOIN lessons l ON l.id = a.lesson_id "
            + "WHERE l.course_id = :courseId", nativeQuery = true)
    List<Long> findUserIdsByCourseId(@Param("courseId") Long courseId);

    /** Job dọn: bản không đụng tới quá lâu. */
    @Modifying
    @Query("DELETE FROM LessonAutosave a WHERE a.updatedAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
