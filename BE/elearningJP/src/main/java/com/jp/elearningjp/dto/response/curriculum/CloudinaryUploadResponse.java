package com.jp.elearningjp.dto.response.curriculum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả trả về sau khi upload một file lên Cloudinary.
 *
 * <p>Các field lấy trực tiếp từ {@code Map} kết quả của Cloudinary SDK.
 * Xem chi tiết response: <a href="https://cloudinary.com/documentation/image_upload_api_reference">Image Upload API</a>.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryUploadResponse {
    /** public_id — định danh asset trên Cloudinary, dùng để delete. */
    private String publicId;

    /** URL bản gốc (giao thức https). */
    private String url;

    /** URL bản secure (https), ưu tiên dùng cho production. */
    private String secureUrl;

    /** Loại resource: {@code image} | {@code video} | {@code raw}. */
    private String resourceType;

    /** Định dạng file (vd: {@code jpg}, {@code mp4}, {@code pdf}). */
    private String format;

    /** Kích thước file tính theo byte. */
    private Long bytes;
}
