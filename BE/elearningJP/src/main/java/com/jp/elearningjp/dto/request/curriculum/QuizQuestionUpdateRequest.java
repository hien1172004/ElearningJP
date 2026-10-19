package com.jp.elearningjp.dto.request.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request body cho endpoint {@code PUT /api/v1/quiz/questions/{id}}.
 *
 * <p>Cho phép cập nhật <b>một phần</b> (partial update) — chỉ field nào gửi
 * lên mới được thay đổi. Tuy nhiên để đơn giản và tránh confusion về
 * semantics, hiện tại FE gửi <i>toàn bộ</i> các trường cần giữ.</p>
 *
 * <p><b>Quy ước:</b></p>
 * <ul>
 *   <li>{@code questionText} — nếu gửi phải khác rỗng (max 5000 ký tự).</li>
 *   <li>{@code explanation} — cho phép rỗng (xoá giải thích).</li>
 *   <li>{@code orderIndex} — nếu gửi và thay đổi sẽ trigger shift order
 *       của các câu hỏi khác trong quiz (giống create).</li>
 *   <li>{@code options} — nếu gửi sẽ THAY THẾ toàn bộ options hiện tại
 *       (xoá cũ + tạo mới). Nếu null/không gửi → giữ options cũ.</li>
 *   <li>{@code matchingData}, {@code correctTextAnswer} — tuỳ theo
 *       questionType. Có thể null nếu không phải loại câu hỏi tương ứng.</li>
 *   <li>{@code vocabId}, {@code characterId} — cho phép đổi liên kết SRS.
 *       Truyền {@code null} (rõ ràng) sẽ xoá liên kết.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionUpdateRequest {

    @Size(min = 1, max = 5000, message = "Nội dung câu hỏi phải từ 1-5000 ký tự")
    String questionText;

    String explanation;

    /** Nếu null hoặc &lt;= 0 → giữ orderIndex cũ. */
    Integer orderIndex;

    QuizQuestionType questionType;

    /** Đáp án đúng (FILL_IN_THE_BLANK). Truyền null để xoá. */
    String correctTextAnswer;

    /** Cấu trúc matching (MATCHING). Truyền null để xoá. */
    JsonNode matchingData;

    /** Đổi vocab liên kết. Truyền null (rõ ràng) để xoá liên kết. */
    Long vocabId;

    /** Đổi kanji liên kết. Truyền null (rõ ràng) để xoá liên kết. */
    Long characterId;

    /**
     * Options mới. Nếu gửi (kể cả mảng rỗng) sẽ thay thế toàn bộ options cũ.
     * Nếu KHÔNG gửi (null) → giữ options hiện tại.
     */
    @Valid
    List<QuizOptionRequest> options;
}
