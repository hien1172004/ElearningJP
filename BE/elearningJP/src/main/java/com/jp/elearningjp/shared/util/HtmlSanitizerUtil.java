package com.jp.elearningjp.shared.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Bộ lọc HTML an toàn (Sanitizer) đồng bộ với Frontend DOMPurify:
 * Whitelist: p, br, strong, b, u, ruby, rt, span[data-speaker]
 */
public final class HtmlSanitizerUtil {

    private static final Safelist JAPANESE_EXAM_SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "b", "u", "ruby", "rt", "span")
            .addAttributes("span", "data-speaker", "class")
            .addProtocols("a", "href", "http", "https");

    private HtmlSanitizerUtil() {
        // Private constructor for utility class
    }

    /**
     * Làm sạch mã HTML, loại bỏ mọi thẻ/thuộc tính nguy hiểm (script, iframe, style, inline handlers...)
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html.trim(), JAPANESE_EXAM_SAFELIST);
    }

    /**
     * Chuẩn hóa dữ liệu cũ (legacy text có \n hoặc tag thô <u>) thành cấu trúc <p> hợp lệ.
     */
    public static String normalizeLegacy(String raw) {
        if (raw == null || raw.isBlank()) {
            return "<p></p>";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("<p") || trimmed.startsWith("<div")) {
            return sanitize(trimmed);
        }
        String[] lines = trimmed.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String safeLine = sanitize(line);
            sb.append("<p>").append(safeLine.isBlank() ? "<br>" : safeLine).append("</p>");
        }
        return sb.toString();
    }
}
