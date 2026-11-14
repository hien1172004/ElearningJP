package com.jp.elearningjp.shared.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public final class PayosChecksumUtil {

    private PayosChecksumUtil() {}

    /**
     * Tạo chữ ký HMAC_SHA256 cho yêu cầu tạo link thanh toán PayOS
     */
    public static String createSignature(long amount, String cancelUrl, String description, long orderCode, String returnUrl, String checksumKey) {
        if (checksumKey == null || checksumKey.isBlank()) {
            return "";
        }
        // Chuỗi dữ liệu sắp xếp theo alphabet đúng chuẩn tài liệu PayOS:
        // amount={amount}&cancelUrl={cancelUrl}&description={description}&orderCode={orderCode}&returnUrl={returnUrl}
        String data = "amount=" + amount +
                "&cancelUrl=" + cancelUrl +
                "&description=" + description +
                "&orderCode=" + orderCode +
                "&returnUrl=" + returnUrl;

        return hmacSha256(data, checksumKey);
    }

    /**
     * Xác thực chữ ký webhook từ PayOS
     */
    public static boolean verifyWebhookSignature(Map<String, Object> data, String signature, String checksumKey) {
        if (checksumKey == null || checksumKey.isBlank() || signature == null) {
            return true; // Nếu chưa cấu hình checksum key trong dev thì bỏ qua xác thực chữ ký
        }
        // Sắp xếp các key trong data theo bảng chữ cái
        Map<String, Object> sortedMap = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (entry.getValue() != null && !entry.getKey().equalsIgnoreCase("signature")) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        String expectedSignature = hmacSha256(sb.toString(), checksumKey);
        return expectedSignature.equalsIgnoreCase(signature);
    }

    public static String hmacSha256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }
}

