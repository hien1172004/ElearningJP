package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1 dòng trong bảng "Top 30 từ đã tra" (Khoi 4).
 *
 * <p><b>Nguồn dữ liệu:</b> bảng append-only {@code study_events}, lọc
 * {@code event_type = 'LOOKUP'} rồi {@code GROUP BY (target_type, target_id,
 * source_ref)} và đếm.</p>
 *
 * <p><b>Cần cả {@code targetType} + {@code targetId} + {@code sourceRef}</b>
 * vì 3 loại đối tượng được tra khác nhau:</p>
 * <ul>
 *   <li>{@code VOCAB} → {@code targetId} = {@code vocabulary.id}, hiển thị
 *       {@code word} + {@code hiragana}.</li>
 *   <li>{@code GRAMMAR} → {@code targetId} = {@code grammar_points.id}, hiển thị
 *       {@code title}.</li>
 *   <li>{@code KANJI} → {@code targetId} luôn {@code null} (chữ Hán là chuỗi),
 *       ký tự nằm trong {@code sourceRef}.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "1 từ/đối tượng được tra nhiều nhất")
public class TopLookupResponse {

    @Schema(description = "Loại đối tượng: VOCAB | GRAMMAR | KANJI", example = "VOCAB")
    private String targetType;

    @Schema(description = "ID đối tượng (null với KANJI)", example = "1024")
    private Long targetId;

    @Schema(description = "Ký tự Hán (chỉ có với KANJI)", example = "日")
    private String kanjiCharacter;

    /** Từ viết (VOCAB) hoặc tên điểm ngữ pháp (GRAMMAR). */
    @Schema(description = "Từ viết / tên ngữ pháp", example = "学校")
    private String label;

    /** Cách đọc kana (VOCAB) hoặc cấu trúc ngữ pháp rút gọn (GRAMMAR). */
    @Schema(description = "Cách đọc kana / cấu trúc", example = "がっこう")
    private String subLabel;

    /** Nghĩa tiếng Việt. */
    @Schema(description = "Nghĩa tiếng Việt", example = "trường học")
    private String meaningVi;

    /** Hán Việt (VOCAB) hoặc mức JLPT dạng chữ (KANJI / GRAMMAR). */
    @Schema(description = "Hán Việt / JLPT", example = "N5")
    private String extraLabel;

    @Schema(description = "Số lần đã tra", example = "12")
    private Integer lookupCount;

    /** URL ảnh Hán Việt hoặc màu bậc JLPT — dùng cho chip màu bậc. */
    @Schema(description = "URL ảnh Hán Việt", example = "https://.../kanji.jpg")
    private String imageUrl;

    @Schema(description = "Mức JLPT (N5..N1), null nếu không có", example = "N5")
    private String jlptLevel;
}