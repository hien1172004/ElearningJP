package com.jp.elearningjp.shared.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility chuyển đổi chữ Kana (Hiragana, Katakana) sang Romaji (Hepburn kiểu gõ phím: trường âm
 * viết lặp nguyên âm, vd がっこう → gakkou — khớp với dữ liệu romaji trong từ điển)
 * và kiểm tra Hán tự (Kanji).
 */
public final class JapaneseKanaUtil {

    private JapaneseKanaUtil() {}

    private static final Map<String, String> KANA_TO_ROMAJI = new HashMap<>();

    private static void put(String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            KANA_TO_ROMAJI.put(pairs[i], pairs[i + 1]);
        }
    }

    static {
        // Nguyên âm và âm cơ bản
        put("あ", "a", "い", "i", "う", "u", "え", "e", "お", "o");
        put("か", "ka", "き", "ki", "く", "ku", "け", "ke", "こ", "ko");
        put("さ", "sa", "し", "shi", "す", "su", "せ", "se", "そ", "so");
        put("た", "ta", "ち", "chi", "つ", "tsu", "て", "te", "と", "to");
        put("な", "na", "に", "ni", "ぬ", "nu", "ね", "ne", "の", "no");
        put("は", "ha", "ひ", "hi", "ふ", "fu", "へ", "he", "ほ", "ho");
        put("ま", "ma", "み", "mi", "む", "mu", "め", "me", "も", "mo");
        put("や", "ya", "ゆ", "yu", "よ", "yo");
        put("ら", "ra", "り", "ri", "る", "ru", "れ", "re", "ろ", "ro");
        put("わ", "wa", "を", "wo", "ん", "n", "ゐ", "i", "ゑ", "e");

        // Biến âm (Dakuon) + bán biến âm (Handakuon)
        put("が", "ga", "ぎ", "gi", "ぐ", "gu", "げ", "ge", "ご", "go");
        put("ざ", "za", "じ", "ji", "ず", "zu", "ぜ", "ze", "ぞ", "zo");
        put("だ", "da", "ぢ", "ji", "づ", "zu", "で", "de", "ど", "do");
        put("ば", "ba", "び", "bi", "ぶ", "bu", "べ", "be", "ぼ", "bo");
        put("ぱ", "pa", "ぴ", "pi", "ぷ", "pu", "ぺ", "pe", "ぽ", "po");
        put("ゔ", "vu");

        // Kana nhỏ đứng riêng (không ghép được với ký tự trước)
        put("ぁ", "a", "ぃ", "i", "ぅ", "u", "ぇ", "e", "ぉ", "o",
                "ゃ", "ya", "ゅ", "yu", "ょ", "yo", "ゎ", "wa");

        // Âm ghép (Yōon)
        put("きゃ", "kya", "きゅ", "kyu", "きょ", "kyo");
        put("しゃ", "sha", "しゅ", "shu", "しょ", "sho");
        put("ちゃ", "cha", "ちゅ", "chu", "ちょ", "cho");
        put("にゃ", "nya", "にゅ", "nyu", "にょ", "nyo");
        put("ひゃ", "hya", "ひゅ", "hyu", "ひょ", "hyo");
        put("みゃ", "mya", "みゅ", "myu", "みょ", "myo");
        put("りゃ", "rya", "りゅ", "ryu", "りょ", "ryo");
        put("ぎゃ", "gya", "ぎゅ", "gyu", "ぎょ", "gyo");
        put("じゃ", "ja", "じゅ", "ju", "じょ", "jo");
        put("ぢゃ", "ja", "ぢゅ", "ju", "ぢょ", "jo");
        put("びゃ", "bya", "びゅ", "byu", "びょ", "byo");
        put("ぴゃ", "pya", "ぴゅ", "pyu", "ぴょ", "pyo");

        // Âm mở rộng (thường gặp trong từ ngoại lai viết Katakana)
        put("しぇ", "she", "じぇ", "je", "ちぇ", "che", "いぇ", "ye");
        put("ふぁ", "fa", "ふぃ", "fi", "ふぇ", "fe", "ふぉ", "fo", "ふゅ", "fyu");
        put("てぃ", "ti", "でぃ", "di", "とぅ", "tu", "どぅ", "du", "でゅ", "dyu");
        put("つぁ", "tsa", "つぃ", "tsi", "つぇ", "tse", "つぉ", "tso");
        put("うぃ", "wi", "うぇ", "we", "うぉ", "wo");
        put("ゔぁ", "va", "ゔぃ", "vi", "ゔぇ", "ve", "ゔぉ", "vo");
    }

    /**
     * Chuyển ký tự Katakana sang Hiragana (giữ nguyên dấu trường âm ー và ký tự khác).
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
     * Chuẩn hoá cách đọc do người dùng nhập để so khớp với từ điển: NFKC (Katakana nửa độ rộng → đầy đủ),
     * bỏ mọi khoảng trắng (kể cả khoảng trắng toàn góc), Katakana → Hiragana. Trống → null.
     */
    public static String normalizeKana(String text) {
        if (text == null) return null;
        String s = Normalizer.normalize(text, Normalizer.Form.NFKC).replaceAll("[\\s\\u3000]+", "");
        s = katakanaToHiragana(s);
        return s.isEmpty() ? null : s;
    }

    /**
     * Kiểm tra chuỗi có chứa chữ Hán (Kanji) hay không.
     */
    public static boolean containsKanji(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(text.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || text.charAt(i) == '々') {
                return true;
            }
        }
        return false;
    }

    /**
     * Chuyển Kana (Hiragana/Katakana) sang Romaji. Ký tự không phải kana được giữ nguyên.
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

            // Xử lý sokuon (âm ngắt っ): nhân đôi phụ âm đầu của âm tiếp theo
            if (c == 'っ') {
                String next = findRomaji(hira, i + 1);
                if (!next.isEmpty() && !isVowel(next.charAt(0))) {
                    sb.append(next.startsWith("ch") ? 't' : next.charAt(0));
                }
                i++;
                continue;
            }

            // Xử lý trường âm ー: lặp lại nguyên âm cuối
            if (c == 'ー') {
                char prev = sb.length() > 0 ? sb.charAt(sb.length() - 1) : 0;
                sb.append(isVowel(prev) ? String.valueOf(prev) : "-");
                i++;
                continue;
            }

            // Thử âm ghép 2 ký tự (きゃ, ふぁ, ...) rồi âm đơn
            String romaji = findRomaji(hira, i);
            if (!romaji.isEmpty()) {
                sb.append(romaji);
                i += (i + 1 < len && KANA_TO_ROMAJI.containsKey(hira.substring(i, i + 2))) ? 2 : 1;
            } else {
                sb.append(c);
                i++;
            }
        }

        return sb.toString();
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
    }

    private static String findRomaji(String hira, int index) {
        if (index >= hira.length()) return "";
        if (index + 1 < hira.length()) {
            String pair = hira.substring(index, index + 2);
            if (KANA_TO_ROMAJI.containsKey(pair)) {
                return KANA_TO_ROMAJI.get(pair);
            }
        }
        return KANA_TO_ROMAJI.getOrDefault(String.valueOf(hira.charAt(index)), "");
    }
}
