package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrammarPointRepository extends JpaRepository<GrammarPoint, Long> {

    Optional<GrammarPoint> findByIdAndDeletedFalse(Long id);

    /**
     * Tra cứu nhanh điểm ngữ pháp theo từ khóa tiêu đề, cấu trúc hoặc ý nghĩa tiếng Việt.
     */
    @Query("SELECT g FROM GrammarPoint g WHERE g.deleted = false AND g.active = true AND (" +
           "LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.structure) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<GrammarPoint> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tra cứu điểm ngữ pháp phân trang kèm bộ lọc JLPT và sắp xếp ưu tiên chính xác:
     * 1. Khớp chính xác tên ngữ pháp
     * 2. Bắt đầu bằng tên ngữ pháp
     * 3. Khớp cấu trúc
     */
    @Query(value = "SELECT g FROM GrammarPoint g WHERE g.deleted = false AND g.active = true AND " +
                   "(:jlptLevel IS NULL OR g.jlptLevel = :jlptLevel) AND (" +
                   "LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(g.structure) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(g.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "ORDER BY " +
                   "CASE " +
                   "  WHEN LOWER(g.title) = LOWER(:keyword) THEN 1 " +
                   "  WHEN LOWER(g.title) LIKE LOWER(CONCAT(:keyword, '%')) THEN 2 " +
                   "  ELSE 3 " +
                   "END ASC, g.id ASC",
           countQuery = "SELECT COUNT(g) FROM GrammarPoint g WHERE g.deleted = false AND g.active = true AND " +
                        "(:jlptLevel IS NULL OR g.jlptLevel = :jlptLevel) AND (" +
                        "LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(g.structure) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(g.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<GrammarPoint> searchWithFilter(@Param("keyword") String keyword,
                                        @Param("jlptLevel") JlptLevel jlptLevel,
                                        Pageable pageable);
}
