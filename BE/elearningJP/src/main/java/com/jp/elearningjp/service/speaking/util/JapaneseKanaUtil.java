package com.jp.elearningjp.service.speaking.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility chuyển đổi chữ Kana (Hiragana, Katakana) sang Romaji (Hepburn)
 * và kiểm tra Hán tự (Kanji).
 */
public final class JapaneseKanaUtil {

    private JapaneseKanaUtil() {}

    private static final Map<String, String> KANA_TO_ROMAJI = new HashMap<>();

    static {
        // Nguyên âm và âm cơ bản
        KANA_TO_ROMAJI.put("あ", "a"); KANA_TO_ROMAJI.put("い", "i"); KANA_TO_ROMAJI.put("う", "u"); KANA_TO_ROMAJI.put("え", "e"); KANA_TO_ROMAJI.put("お", "o");
        KANA_TO_ROMAJI.put("か", "ka"); KANA_TO_ROMAJI.put("き", "ki"); KANA_TO_ROMAJI.put("く", "ku"); KANA_TO_ROMAJI.put("け", "ke"); KANA_TO_ROMAJI.put("こ", "ko");
        KANA_TO_ROMAJI.put("さ", "sa"); KANA_TO_ROMAJI.put("し", "shi"); KANA_TO_ROMAJI.put("す", "su"); KANA_TO_ROMAJI.put("せ", "se"); KANA_TO_ROMAJI.put("そ", "so");
        KANA_TO_ROMAJI.put("た", "ta"); KANA_TO_ROMAJI.put("ち", "chi"); KANA_TO_ROMAJI.put("つ", "tsu"); KANA_TO_ROMAJI.put("て", "te"); KANA_TO_ROMAJI.put("と", "to");
        KANA_TO_ROMAJI.put("な", "na"); KANA_TO_ROMAJI.put("に", "ni"); KANA_TO_ROMAJI.put("ぬ", "nu"); KANA_TO_ROMAJI.put("ね", "ne"); KANA_TO_ROMAJI.put("の", "no");
        KANA_TO_ROMAJI.put("は", "ha"); KANA_TO_ROMAJI.put("ひ", "hi"); KANA_TO_ROMAJI.put("ふ", "fu"); KANA_TO_ROMAJI.put("へ", "he"); KANA_TO_ROMAJI.put("ほ", "ho");
        KANA_TO_ROMAJI.put("ま", "ma"); KANA_TO_ROMAJI.put("み", "mi"); KANA_TO_ROMAJI.put("む", "mu"); KANA_TO_ROMAJI.put("め", "me"); KANA_TO_ROMAJI.put("も", "mo");
        KANA_TO_ROMAJI.put("や", "ya"); KANA_TO_ROMAJI.put("ゆ", "yu"); KANA_TO_ROMAJI.put("よ", "yo");
        KANA_TO_ROMAJI.put("ら", "ra"); KANA_TO_ROMAJI.put("り", "ri"); KANA_TO_ROMAJI.put("る", "ru"); KANA_TO_ROMAJI.put("れ", "re"); KANA_TO_ROMAJI.put("ろ", "ro");
        KANA_TO_ROMAJI.put("わ", "wa"); KANA_TO_ROMAJI.put("を", "wo"); KANA_TO_ROMAJI.put("ん", "n");

        // Biến âm (Dakuon)
        KANA_TO_ROMAJI.put("が", "ga"); KANA_TO_ROMAJI.put("ぎ", "gi"); KANA_TO_ROMAJI.put("ぐ", "gu"); KANA_TO_ROMAJI.put("げ", "ge"); KANA_TO_ROMAJI.put("ご", "go");
        KANA_TO_ROMAJI.put("ざ", "za"); KANA_TO_ROMAJI.put("じ", "ji"); KANA_TO_ROMAJI.put("ず", "zu"); KANA_TO_ROMAJI.put("ぜ", "ze"); KANA_TO_ROMAJI.put("ぞ", "zo");
        KANA_TO_ROMAJI.put("だ", "da"); KANA_TO_ROMAJI.put("ぢ", "ji"); KANA_TO_ROMAJI.put("づ", "zu"); KANA_TO_ROMAJI.put("で", "de"); KANA_TO_ROMAJI.put("ど", "do");
        KANA_TO_ROMAJI.put("ば", "ba"); KANA_TO_ROMAJI.put("び", "bi"); KANA_TO_ROMAJI.put("ぶ", "bu"); KANA_TO_ROMAJI.put("べ", "be"); KANA_TO_ROMAJI.put("ぼ", "bo");

        // Bán biến âm (Handakuon)
        KANA_TO_ROMAJI.put("ぱ", "pa"); KANA_TO_ROMAJI.put("ぴ", "pi"); KANA_TO_ROMAJI.put("ぷ", "pu"); KANA_TO_ROMAJI.put("ぺ", "pe"); KANA_TO_ROMAJI.put("ぽ", "po");

        // Âm ghép (Yōon)
        KANA_TO_ROMAJI.put("きゃ", "kya"); KANA_TO_ROMAJI.put("きゅ", "kyu"); KANA_TO_ROMAJI.put("きょ", "kyo");
        KANA_TO_ROMAJI.put("しゃ", "sha"); KANA_TO_ROMAJI.put("しゅ", "shu"); KANA_TO_ROMAJI.put("しょ", "sho");
        KANA_TO_ROMAJI.put("ちゃ", "cha"); KANA_TO_ROMAJI.put("ちゅ", "chu"); KANA_TO_ROMAJI.put("ちょ", "cho");
        KANA_TO_ROMAJI.put("にゃ", "nya"); KANA_TO_ROMAJI.put("にゅ", "nyu"); KANA_TO_ROMAJI.put("にょ", "nyo");
        KANA_TO_ROMAJI.put("ひゃ", "hya"); KANA_TO_ROMAJI.put("ひゅ", "hyu"); KANA_TO_ROMAJI.put("ひょ", "hyo");
        KANA_TO_ROMAJI.put("みゃ", "mya"); KANA_TO_ROMAJI.put("みゅ", "myu"); KANA_TO_ROMAJI.put("みょ", "myo");
        KANA_TO_ROMAJI.put("りゃ", "rya"); KANA_TO_ROMAJI.put("りゅ", "ryu"); KANA_TO_ROMAJI.put("りょ", "ryo");
        KANA_TO_ROMAJI.put("ぎゃ", "gya"); KANA_TO_ROMAJI.put("ぎゅ", "gyu"); KANA_TO_ROMAJI.put("ぎょ", "gyo");
        KANA_TO_ROMAJI.put("じゃ", "ja");  KANA_TO_ROMAJI.put("じゅ", "ju");  KANA_TO_ROMAJI.put("じょ", "jo");
        KANA_TO_ROMAJI.put("びゃ", "bya"); KANA_TO_ROMAJI.put("びゅ", "byu"); KANA_TO_ROMAJI.put("びょ", "byo");
        KANA_TO_ROMAJI.put("ぴゃ", "pya"); KANA_TO_ROMAJI.put("ぴゅ", "pyu"); KANA_TO_ROMAJI.put("ぴょ", "pyo");
    }

    /**
     * Chuyển ký tự Katakana sang Hiragana.
     */
    public static String katakanaToHiragana(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x30A1 && c <= 0x30F6) {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Kiểm tra chuỗi có chứa chữ Hán (Kanji) hay không.
     */
    public static boolean containsKanji(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chuyển Hiragana sang Romaji chuẩn Hepburn.
     */
    public static String kanaToRomaji(String kana) {
        if (kana == null) return "";
        String hira = katakanaToHiragana(kana).trim();
        if ("こんにちは".equals(hira)) return "konnichiwa";
        if ("こんばんは".equals(hira)) return "konbanwa";
        StringBuilder sb = new StringBuilder();

        int i = 0;
        int len = hira.length();

        while (i < len) {
            char c = hira.charAt(i);

            // Xử lý sokuon (âm ngắt っ)
            if (c == 'っ' || c == 'ッ') {
                if (i + 1 < len) {
                    String next = findRomaji(hira, i + 1);
                    if (!next.isEmpty()) {
                        char firstChar = next.charAt(0);
                        if (next.startsWith("ch")) {
                            sb.append("t");
                        } else {
                            sb.append(firstChar);
                        }
                    }
                }
                i++;
                continue;
            }

            // Xử lý trường âm ー
            if (c == 'ー') {
                if (sb.length() > 0) {
                    char prev = sb.charAt(sb.length() - 1);
                    sb.append(prev);
                } else {
                    sb.append("-");
                }
                i++;
                continue;
            }

            // Thử kiểm tra âm ghép 2 ký tự (きゃ, しゃ, ...)
            if (i + 1 < len) {
                String pair = hira.substring(i, i + 2);
                if (KANA_TO_ROMAJI.containsKey(pair)) {
                    sb.append(KANA_TO_ROMAJI.get(pair));
                    i += 2;
                    continue;
                }
            }

            // Âm đơn 1 ký tự
            String single = String.valueOf(c);
            if (KANA_TO_ROMAJI.containsKey(single)) {
                sb.append(KANA_TO_ROMAJI.get(single));
            } else {
                sb.append(c);
            }
            i++;
        }

        return sb.toString();
    }

    private static String findRomaji(String hira, int index) {
        if (index >= hira.length()) return "";
        if (index + 1 < hira.length()) {
            String pair = hira.substring(index, index + 2);
            if (KANA_TO_ROMAJI.containsKey(pair)) {
                return KANA_TO_ROMAJI.get(pair);
            }
        }
        String single = String.valueOf(hira.charAt(index));
        return KANA_TO_ROMAJI.getOrDefault(single, "");
    }
}
