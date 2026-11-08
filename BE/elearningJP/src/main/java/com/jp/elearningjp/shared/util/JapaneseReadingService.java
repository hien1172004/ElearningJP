package com.jp.elearningjp.shared.util;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sinh cách đọc (Hiragana) và Romaji cho văn bản tiếng Nhật bằng Kuromoji (từ điển ipadic)
 * + {@link JapaneseKanaUtil}. Dùng chung 1 {@link Tokenizer} cho toàn ứng dụng (furigana speaking,
 * import Excel bài học...).
 *
 * <p>Lưu ý: Kuromoji tách câu theo ngữ cảnh nên đọc câu khá chuẩn, nhưng với 1 từ đơn đứng riêng
 * có thể chọn sai cách đọc (vd 一日 → いちにち/ついたち). Vì vậy nơi gọi nên ưu tiên dữ liệu
 * do người dùng nhập / từ điển, chỉ dùng kết quả ở đây khi thiếu.</p>
 */
@Slf4j
@Component
public class JapaneseReadingService {

    private final Tokenizer tokenizer;

    public JapaneseReadingService() {
        this.tokenizer = new Tokenizer();
        log.info("Kuromoji tokenizer initialized successfully.");
    }

    public List<Token> tokenize(String text) {
        return tokenizer.tokenize(text);
    }

    /** Cách đọc Hiragana của 1 token: token có Kanji lấy reading của Kuromoji, còn lại lấy chính mặt chữ. */
    public String tokenReading(Token token) {
        String surface = token.getSurface();
        String reading = token.getReading();
        if (JapaneseKanaUtil.containsKanji(surface) && reading != null && !"*".equals(reading)) {
            return JapaneseKanaUtil.katakanaToHiragana(reading);
        }
        return JapaneseKanaUtil.katakanaToHiragana(surface);
    }

    /** Romaji của 1 token, xử lý trợ từ は → wa, へ → e. Token Kanji không có reading giữ nguyên mặt chữ. */
    public String tokenRomaji(Token token) {
        String surface = token.getSurface();
        if (JapaneseKanaUtil.containsKanji(surface)) {
            String reading = token.getReading();
            return reading != null && !"*".equals(reading)
                    ? JapaneseKanaUtil.kanaToRomaji(JapaneseKanaUtil.katakanaToHiragana(reading))
                    : surface;
        }
        if ("助詞".equals(token.getPartOfSpeechLevel1())) {
            if ("は".equals(surface)) return "wa";
            if ("へ".equals(surface)) return "e";
        }
        return JapaneseKanaUtil.kanaToRomaji(surface);
    }

    /** Cách đọc Hiragana của cả chuỗi (không có khoảng trắng). Trống → null. */
    public String toHiragana(String text) {
        if (text == null || text.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        for (Token token : tokenize(text.strip())) {
            sb.append(tokenReading(token));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /** Romaji của cả câu, các từ cách nhau 1 khoảng trắng, dấu câu dính liền từ trước. Trống → null. */
    public String toRomaji(String text) {
        if (text == null || text.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        for (Token token : tokenize(text.strip())) {
            String romaji = tokenRomaji(token);
            if (!sb.isEmpty() && !romaji.isBlank() && !isPunctuation(token.getSurface())) {
                sb.append(' ');
            }
            sb.append(romaji);
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    public static boolean isPunctuation(String surface) {
        return surface.matches("^[、。！？,.!? ]+$");
    }
}
