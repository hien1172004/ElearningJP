package com.jp.elearningjp.service.speaking.impl;

import com.atilika.kuromoji.ipadic.Token;
import com.jp.elearningjp.dto.response.speaking.FuriganaResponse;
import com.jp.elearningjp.dto.response.speaking.FuriganaTokenDto;
import com.jp.elearningjp.service.speaking.FuriganaService;
import com.jp.elearningjp.shared.util.JapaneseKanaUtil;
import com.jp.elearningjp.shared.util.JapaneseReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Sinh furigana (thẻ ruby) + romaji cho câu tiếng Nhật, dùng Kuromoji qua {@link JapaneseReadingService}. */
@Service
@RequiredArgsConstructor
public class FuriganaServiceImpl implements FuriganaService {

    private final JapaneseReadingService readingService;

    @Override
    public FuriganaResponse parseFurigana(String textJa) {
        if (textJa == null || textJa.isBlank()) {
            return FuriganaResponse.builder()
                    .original("")
                    .rubyHtml("")
                    .romaji("")
                    .tokens(new ArrayList<>())
                    .build();
        }

        List<Token> kuromojiTokens = readingService.tokenize(textJa);
        List<FuriganaTokenDto> tokenDtos = new ArrayList<>();
        StringBuilder rubyBuilder = new StringBuilder();
        StringBuilder romajiBuilder = new StringBuilder();

        for (Token token : kuromojiTokens) {
            String surface = token.getSurface();
            String rawReading = token.getReading();
            boolean hasKanji = JapaneseKanaUtil.containsKanji(surface);
            String romaji = readingService.tokenRomaji(token);
            String furigana = null;

            if (hasKanji && rawReading != null && !rawReading.equals("*")) {
                furigana = JapaneseKanaUtil.katakanaToHiragana(rawReading);

                // Tách okurigana đuôi (ví dụ: surface '食べ', reading 'たべ' -> '<ruby>食<rt>た</rt></ruby>べ')
                int trailingKanaCount = 0;
                while (trailingKanaCount < surface.length() && trailingKanaCount < furigana.length()) {
                    char sChar = surface.charAt(surface.length() - 1 - trailingKanaCount);
                    char fChar = furigana.charAt(furigana.length() - 1 - trailingKanaCount);
                    if (sChar == fChar && !JapaneseKanaUtil.containsKanji(String.valueOf(sChar))) {
                        trailingKanaCount++;
                    } else {
                        break;
                    }
                }

                if (trailingKanaCount > 0) {
                    String kanjiPart = surface.substring(0, surface.length() - trailingKanaCount);
                    String rtPart = furigana.substring(0, furigana.length() - trailingKanaCount);
                    String okurigana = surface.substring(surface.length() - trailingKanaCount);
                    rubyBuilder.append("<ruby>").append(kanjiPart).append("<rt>").append(rtPart).append("</rt></ruby>").append(okurigana);
                } else {
                    rubyBuilder.append("<ruby>").append(surface).append("<rt>").append(furigana).append("</rt></ruby>");
                }
            } else {
                rubyBuilder.append(surface);
            }

            if (romajiBuilder.length() > 0 && !romaji.isBlank() && !JapaneseReadingService.isPunctuation(surface)) {
                romajiBuilder.append(" ");
            }
            romajiBuilder.append(romaji);

            tokenDtos.add(FuriganaTokenDto.builder()
                    .surface(surface)
                    .furigana(furigana)
                    .romaji(romaji)
                    .isKanji(hasKanji)
                    .build());
        }

        return FuriganaResponse.builder()
                .original(textJa)
                .rubyHtml(rubyBuilder.toString())
                .romaji(romajiBuilder.toString().trim())
                .tokens(tokenDtos)
                .build();
    }

    @Override
    public String toRubyHtml(String textJa) {
        return parseFurigana(textJa).getRubyHtml();
    }

    @Override
    public String toRomaji(String textJa) {
        return parseFurigana(textJa).getRomaji();
    }
}
