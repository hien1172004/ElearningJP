package com.jp.elearningjp.dto.request.speaking;

import com.jp.elearningjp.shared.enums.JlptLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Nội dung đầy đủ của một kịch bản luyện nói (tạo mới / cập nhật từ trang quản lý).
 * Cấu trúc các danh sách khớp với JSON đang lưu trong ai_scenario (xem seed V30).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminScenarioRequest {

    @NotBlank(message = "Chủ đề không được để trống")
    @Pattern(regexp = "^[A-Z][A-Z_]{1,49}$", message = "Mã chủ đề chỉ gồm chữ in hoa và dấu gạch dưới")
    String topicId;

    @NotNull(message = "Cấp độ JLPT không được để trống")
    JlptLevel jlptLevel;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @NotBlank(message = "Mô tả tình huống không được để trống")
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    String description;

    @NotBlank(message = "Vai của người học không được để trống")
    @Size(max = 100, message = "Vai của người học tối đa 100 ký tự")
    String userRole;

    @NotBlank(message = "Vai của AI không được để trống")
    @Size(max = 100, message = "Vai của AI tối đa 100 ký tự")
    String aiRole;

    @NotEmpty(message = "Cần ít nhất 1 nhiệm vụ")
    @Size(max = 10, message = "Tối đa 10 nhiệm vụ")
    @Builder.Default
    List<@Valid TaskItem> tasks = new ArrayList<>();

    @Size(max = 30, message = "Tối đa 30 câu hội thoại mẫu")
    @Builder.Default
    List<@Valid DialogueLine> sampleDialogue = new ArrayList<>();

    @Size(max = 50, message = "Tối đa 50 từ vựng")
    @Builder.Default
    List<@Valid VocabItem> vocabulary = new ArrayList<>();

    @Size(max = 20, message = "Tối đa 20 mẫu ngữ pháp")
    @Builder.Default
    List<@Valid GrammarItem> grammar = new ArrayList<>();

    @Size(max = 10, message = "Tối đa 10 mẹo")
    @Builder.Default
    List<@NotBlank(message = "Mẹo không được để trống") @Size(max = 500, message = "Mỗi mẹo tối đa 500 ký tự") String> tips = new ArrayList<>();

    Boolean isPremium;

    Boolean isLessonOnly;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "URL ảnh không hợp lệ")
    String thumbnailUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TaskItem {
        @NotBlank(message = "Nội dung nhiệm vụ không được để trống")
        @Size(max = 300, message = "Mỗi nhiệm vụ tối đa 300 ký tự")
        String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DialogueLine {
        @NotBlank(message = "Người nói không được để trống")
        @Pattern(regexp = "^(AI|USER)$", message = "Người nói phải là AI hoặc USER")
        String speaker;

        @NotBlank(message = "Câu thoại không được để trống")
        @Size(max = 500, message = "Mỗi câu thoại tối đa 500 ký tự")
        String text;

        /** Nghĩa tiếng Việt (không bắt buộc). */
        @Size(max = 500, message = "Bản dịch tối đa 500 ký tự")
        String vi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VocabItem {
        @NotBlank(message = "Từ vựng không được để trống")
        @Size(max = 100, message = "Từ vựng tối đa 100 ký tự")
        String word;

        @Size(max = 100, message = "Cách đọc tối đa 100 ký tự")
        String reading;

        @NotBlank(message = "Nghĩa của từ không được để trống")
        @Size(max = 300, message = "Nghĩa tối đa 300 ký tự")
        String meaning;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GrammarItem {
        @NotBlank(message = "Mẫu ngữ pháp không được để trống")
        @Size(max = 200, message = "Mẫu ngữ pháp tối đa 200 ký tự")
        String pattern;

        @NotBlank(message = "Ý nghĩa ngữ pháp không được để trống")
        @Size(max = 300, message = "Ý nghĩa tối đa 300 ký tự")
        String meaning;

        @Size(max = 300, message = "Ví dụ tối đa 300 ký tự")
        String example;

        @Size(max = 300, message = "Nghĩa ví dụ tối đa 300 ký tự")
        String exampleMeaning;
    }
}
