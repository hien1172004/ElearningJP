package com.jp.elearningjp.service.speaking.impl;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.jp.elearningjp.dto.response.speaking.FuriganaResponse;
import com.jp.elearningjp.dto.response.speaking.FuriganaTokenDto;
import com.jp.elearningjp.service.speaking.FuriganaService;
import com.jp.elearningjp.service.speaking.util.JapaneseKanaUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FuriganaServiceImpl implements FuriganaService {

    private Tokenizer tokenizer;

    @PostConstruct
    public void init() {
        try {
            this.tokenizer = new Tokenizer();
            log.info("Kuromoji tokenizer initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Kuromoji tokenizer: {}", e.getMessage(), e);
        }
    }

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

        if (tokenizer == null) {
            tokenizer = new Tokenizer();
        }

        List<Token> kuromojiTokens = tokenizer.tokenize(textJa);
        List<FuriganaTokenDto> tokenDtos = new ArrayList<>();
        StringBuilder rubyBuilder = new StringBuilder();
        StringBuilder romajiBuilder = new StringBuilder();

        for (Token token : kuromojiTokens) {
            String surface = token.getSurface();
            String rawReading = token.getReading();
            boolean hasKanji = JapaneseKanaUtil.containsKanji(surface);

            String furigana = null;
            String romaji;

            if (hasKanji) {
                if (rawReading != null && !rawReading.equals("*")) {
                    furigana = JapaneseKanaUtil.katakanaToHiragana(rawReading);
                    romaji = JapaneseKanaUtil.kanaToRomaji(furigana);

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
                    romaji = surface;
                    rubyBuilder.append(surface);
                }
            } else {
                if ("助詞".equals(token.getPartOfSpeechLevel1())) {
                    if ("は".equals(surface)) romaji = "wa";
                    else if ("へ".equals(surface)) romaji = "e";
                    else romaji = JapaneseKanaUtil.kanaToRomaji(surface);
                } else {
                    romaji = JapaneseKanaUtil.kanaToRomaji(surface);
                }
                rubyBuilder.append(surface);
            }

            if (romajiBuilder.length() > 0 && !romaji.isBlank() && !surface.matches("^[、。！？,.!? ]+$")) {
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
