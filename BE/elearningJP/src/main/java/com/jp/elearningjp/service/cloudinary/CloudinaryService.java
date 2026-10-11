package com.jp.elearningjp.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jp.elearningjp.dto.response.curriculum.CloudinaryUploadResponse;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Service thao tác với Cloudinary: upload và delete asset.
 *
 * <p>Được inject vào các service / controller có nhu cầu upload media
 * (vd: thumbnail khóa học, video / tài liệu bài học, avatar người dùng).
 *
 * <p>Cloudinary client được cấu hình trong {@link com.jp.elearningjp.config.CloudinaryConfig}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload một file lên Cloudinary.
     *
     * @param file         file được upload từ request (multipart)
     * @param folder       thư mục lưu trữ trên Cloudinary (vd: {@code "courses/thumbnails"}).
     *                     Có thể {@code null} để upload vào root.
     * @param publicIdHint chuỗi gợi ý để đặt tên asset (vd: slug khóa học).
     *                     Nếu truyền vào sẽ được append UUID để tránh trùng lặp.
     * @return thông tin asset sau khi upload (publicId, url, secureUrl, format, bytes, ...)
     * @throws AppException {@link ErrorCode#UNCAUGHT_EXCEPTION} nếu file rỗng, IO lỗi,
     *                     hoặc Cloudinary trả về lỗi.
     */
    public CloudinaryUploadResponse upload(MultipartFile file, String folder, String publicIdHint) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }

        try {
            // public_id duy nhất: hint + UUID ngắn (nếu có hint), ngược lại để Cloudinary tự sinh.
            String uniquePublicId = publicIdHint != null && !publicIdHint.isBlank()
                    ? publicIdHint + "-" + UUID.randomUUID().toString().substring(0, 8)
                    : UUID.randomUUID().toString();

            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", uniquePublicId,
                    // Tự nhận diện resource_type (image/video/raw) theo nội dung file
                    "resource_type", "auto",
                    // Ghi đè nếu public_id vô tình trùng
                    "overwrite", true,
                    // Cache 1 năm trên CDN của Cloudinary
                    "invalidate", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader()
                    .upload(file.getBytes(), options);

            log.info("Uploaded file to Cloudinary: publicId={}, size={} bytes",
                    result.get("public_id"), result.get("bytes"));

            return CloudinaryUploadResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("url"))
                    .secureUrl((String) result.get("secure_url"))
                    .resourceType((String) result.get("resource_type"))
                    .format((String) result.get("format"))
                    .bytes(toLong(result.get("bytes")))
                    .build();

        } catch (IOException e) {
            log.error("Failed to read uploaded file bytes", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        } catch (Exception e) {
            log.error("Cloudinary upload failed for file: {}", file.getOriginalFilename(), e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    /**
     * Upload file không cần gợi ý publicId (dùng UUID ngẫu nhiên).
     *
     * @param file   file upload từ request
     * @param folder thư mục trên Cloudinary (có thể null)
     */
    public CloudinaryUploadResponse upload(MultipartFile file, String folder) {
        return upload(file, folder, null);
    }

    /**
     * Xóa một asset khỏi Cloudinary theo {@code publicId}.
     *
     * <p>Thao tác này an toàn: nếu asset không tồn tại, Cloudinary trả về
     * {@code result = "not found"} — service vẫn không throw exception, chỉ log cảnh báo.
     *
     * @param publicId     public_id của asset cần xóa
     * @param resourceType loại resource: {@code image} (mặc định) | {@code video} | {@code raw}.
     *                     Có thể {@code null} để mặc định là {@code image}.
     * @return {@code true} nếu xóa thành công, {@code false} nếu không tìm thấy asset.
     * @throws AppException {@link ErrorCode#UNCAUGHT_EXCEPTION} nếu có lỗi từ Cloudinary SDK.
     */
    public boolean delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("Skip Cloudinary delete: publicId is blank");
            return false;
        }

        try {
            String type = (resourceType == null || resourceType.isBlank()) ? "image" : resourceType;
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", type,
                    "invalidate", true
            ));

            Object resultStatus = result.get("result");
            boolean ok = "ok".equals(resultStatus);
            if (!ok) {
                log.warn("Cloudinary delete returned non-ok status: publicId={}, result={}", publicId, resultStatus);
            } else {
                log.info("Deleted asset from Cloudinary: publicId={}, type={}", publicId, type);
            }
            return ok;

        } catch (Exception e) {
            log.error("Cloudinary delete failed for publicId: {}", publicId, e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    /**
     * Xóa asset mặc định (image).
     */
    public boolean delete(String publicId) {
        return delete(publicId, null);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
