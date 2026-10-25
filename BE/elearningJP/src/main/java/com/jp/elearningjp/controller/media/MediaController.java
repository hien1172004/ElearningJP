package com.jp.elearningjp.controller.media;

import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.dto.response.curriculum.CloudinaryUploadResponse;
import com.jp.elearningjp.service.cloudinary.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Upload", description = "API tải lên tệp đa phương tiện (Ảnh, Audio) từ máy tính")
public class MediaController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải file ảnh hoặc audio lên Cloudinary và nhận URL trực tiếp")
    public ApiResponse<CloudinaryUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "elearningJP/exam") String folder
    ) {
        CloudinaryUploadResponse response = cloudinaryService.upload(file, folder);
        return ApiResponse.<CloudinaryUploadResponse>builder()
                .success(true)
                .code(200)
                .message("Tải lên tệp thành công")
                .data(response)
                .build();
    }
}
