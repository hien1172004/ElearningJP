package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Bản nháp theo phía của bài học (V65). Ghi trạng thái bằng UPDATE có điều kiện: 2 thao tác chồng nhau trên cùng
 * nháp được Postgres xếp hàng theo khóa dòng, thao tác sau đánh giá lại WHERE trên dữ liệu đã commit → chỉ 1 bên thắng.
 * Tham số {@code kind} / {@code status} truyền dạng chuỗi (tên enum) vì là câu lệnh SQL thuần.
 */
@Repository
public interface LessonDraftRepository extends JpaRepository<LessonDraft, Long> {

    /** Nháp đang dùng (EDITING / SUBMITTED) của 1 phía. */
    @Query(value = "SELECT * FROM lesson_drafts WHERE lesson_id = :lessonId AND kind = :kind AND status <> 'ARCHIVED'",
            nativeQuery = true)
    Optional<LessonDraft> findActive(@Param("lessonId") Long lessonId, @Param("kind") String kind);

    /** Nháp cũ chỉ đọc (giữ lại khi "cập nhật bản mới"). */
    @Query(value = "SELECT * FROM lesson_drafts WHERE lesson_id = :lessonId AND kind = :kind AND status = 'ARCHIVED'",
            nativeQuery = true)
    Optional<LessonDraft> findArchived(@Param("lessonId") Long lessonId, @Param("kind") String kind);

    /** Nháp đang dùng của 1 phía cho nhiều bài (trạng thái từng bài trong khóa). */
    @Query(value = "SELECT * FROM lesson_drafts WHERE lesson_id IN (:lessonIds) AND kind = :kind "
            + "AND status <> 'ARCHIVED'", nativeQuery = true)
    List<LessonDraft> findActiveByLessonIds(@Param("lessonIds") Collection<Long> lessonIds, @Param("kind") String kind);

    /** Đọc thẳng DB, khóa dòng tới hết giao dịch (xếp hàng với thao tác ghi / gửi duyệt khác trên cùng nháp). */
    @Query(value = "SELECT * FROM lesson_drafts WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<LessonDraft> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT id FROM lesson_drafts WHERE needs_import = TRUE ORDER BY id", nativeQuery = true)
    List<Long> findIdsNeedingImport();

    // =========================================================
    // GHI TRẠNG THÁI
    // =========================================================

    /**
     * Ghi nội dung nháp: tăng {@code draft_revision}, đánh dấu có thay đổi, ghi người / lúc sửa.
     * Trả 0 nếu nháp không còn ở trạng thái soạn (đã gửi duyệt / đã lưu trữ) — caller ném lỗi.
     */
    @Modifying
    @Query(value = """
            UPDATE lesson_drafts SET draft_revision = draft_revision + 1, has_changes = TRUE,
                   updated_by = :userId, updated_by_name = :userName, content_updated_at = :at
            WHERE id = :id AND status = 'EDITING'
            """, nativeQuery = true)
    int markEdited(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("userName") String userName, @Param("at") Instant at);

    /**
     * Gửi duyệt: chỉ khi nháp đang soạn, có thay đổi, dựa trên bản live hiện tại và đúng bản người gửi đang thấy.
     * Trả 0 → caller đọc lại trạng thái để báo lý do.
     */
    @Modifying
    @Query(value = """
            UPDATE lesson_drafts d SET status = 'SUBMITTED'
            FROM lessons l
            WHERE d.id = :id AND l.id = d.lesson_id AND l.deleted = FALSE
              AND d.status = 'EDITING' AND d.has_changes = TRUE AND d.draft_revision = :draftRevision
              AND d.base_live_revision = l.live_revision
            """, nativeQuery = true)
    int lockForReview(@Param("id") Long id, @Param("draftRevision") long draftRevision);

    /** Rút / từ chối: nháp đã gửi quay lại đang soạn (vẫn còn thay đổi). */
    @Modifying
    @Query(value = "UPDATE lesson_drafts SET status = 'EDITING' WHERE id = :id AND status = 'SUBMITTED'",
            nativeQuery = true)
    int unlockReview(@Param("id") Long id);

    /** Nháp vừa được xuất bản: trùng bản live mới → hết thay đổi, dựa trên {@code liveRevision}, mở lại để soạn. */
    @Modifying
    @Query(value = """
            UPDATE lesson_drafts SET status = 'EDITING', has_changes = FALSE, base_live_revision = :liveRevision,
                   draft_revision = draft_revision + 1
            WHERE id = :id
            """, nativeQuery = true)
    int markPublished(@Param("id") Long id, @Param("liveRevision") long liveRevision);

    /** Nháp vừa được dựng lại từ bản live. */
    @Modifying
    @Query(value = """
            UPDATE lesson_drafts SET has_changes = FALSE, needs_import = FALSE, base_live_revision = :liveRevision,
                   draft_revision = draft_revision + 1
            WHERE id = :id
            """, nativeQuery = true)
    int markImported(@Param("id") Long id, @Param("liveRevision") long liveRevision);

    @Modifying
    @Query(value = "UPDATE lesson_drafts SET status = 'ARCHIVED' WHERE id = :id AND status <> 'ARCHIVED'",
            nativeQuery = true)
    int archive(@Param("id") Long id);

    /** Xóa hẳn nháp (cascade toàn bộ nội dung) — dùng cho nháp cũ chỉ đọc bị thay thế. */
    @Modifying
    @Query(value = "DELETE FROM lesson_drafts WHERE id = :id", nativeQuery = true)
    int deleteDraft(@Param("id") Long id);
}
