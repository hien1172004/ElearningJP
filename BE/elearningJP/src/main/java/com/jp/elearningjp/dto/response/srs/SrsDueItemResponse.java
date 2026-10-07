package com.jp.elearningjp.dto.response.srs;

import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsDueItemResponse {

    Long srsItemId;
    SrsCardType cardType;
    String itemType; // VOCABULARY, CHARACTER, GRAMMAR
    SrsStatus status;
    Integer intervalDays;
    Integer repetitionCount;
    BigDecimal easeFactor;
    Instant nextReviewAt;

    // Chi tiết nội dung Flashcard
    Long referenceId;       // vocabId / characterId / grammarId
    String frontText;         // Mặt trước: Từ Kanji/Hiragana hoặc điểm ngữ pháp
    String backMeaning;       // Mặt sau: Nghĩa tiếng Việt
    String reading;           // Romaji / Furigana / Onyomi / Kunyomi
    String audioUrl;          // Phát âm nếu có
    String exampleSentence;   // Câu ví dụ
    String exampleTranslation; // Dịch câu ví dụ
}
