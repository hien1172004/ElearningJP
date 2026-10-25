package com.jp.elearningjp.service.speaking;

import com.jp.elearningjp.dto.response.speaking.FuriganaResponse;
import com.jp.elearningjp.dto.response.speaking.FuriganaTokenDto;
import com.jp.elearningjp.service.speaking.impl.FuriganaServiceImpl;
import com.jp.elearningjp.service.speaking.util.JapaneseKanaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuriganaServiceTest {

    private FuriganaServiceImpl furiganaService;

    @BeforeEach
    void setUp() {
        furiganaService = new FuriganaServiceImpl();
        furiganaService.init();
    }

    @Test
    @DisplayName("Chuyển đổi Kana sang Romaji chuẩn Hepburn")
    void testKanaToRomaji() {
        assertEquals("watashi", JapaneseKanaUtil.kanaToRomaji("わたし"));
        assertEquals("konnichiwa", JapaneseKanaUtil.kanaToRomaji("こんにちは"));
        assertEquals("toukyou", JapaneseKanaUtil.kanaToRomaji("とうきょう"));
        assertEquals("chotto", JapaneseKanaUtil.kanaToRomaji("ちょっと"));
    }

    @Test
    @DisplayName("Kiểm tra Kanji trong văn bản")
    void testContainsKanji() {
        assertTrue(JapaneseKanaUtil.containsKanji("私"));
        assertTrue(JapaneseKanaUtil.containsKanji("ラーメン屋"));
        assertFalse(JapaneseKanaUtil.containsKanji("わたし"));
        assertFalse(JapaneseKanaUtil.containsKanji("ラーメン"));
    }

    @Test
    @DisplayName("Sinh Furigana và thẻ ruby từ Kuromoji thành công")
    void testParseFurigana() {
        String text = "私はラーメンを食べます。";
        FuriganaResponse response = furiganaService.parseFurigana(text);

        assertNotNull(response);
        assertEquals(text, response.getOriginal());
        assertFalse(response.getTokens().isEmpty());

        // Kiểm tra thẻ ruby được tạo cho Kanji
        assertTrue(response.getRubyHtml().contains("<ruby>私<rt>わたし</rt></ruby>"));
        assertTrue(response.getRubyHtml().contains("<ruby>食<rt>た</rt></ruby>べます"));

        // Kiểm tra romaji tổng thể
        assertNotNull(response.getRomaji());
        assertFalse(response.getRomaji().isBlank());
    }
}
