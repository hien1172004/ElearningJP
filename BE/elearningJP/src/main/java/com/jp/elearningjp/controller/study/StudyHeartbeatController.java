package com.jp.elearningjp.controller.study;

import com.jp.elearningjp.dto.request.study.StudyHeartbeatRequest;
import com.jp.elearningjp.dto.response.study.StudyHeartbeatResponse;
import com.jp.elearningjp.service.study.StudyHeartbeatService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint heartbeat thời gian học của user (Bước 3 / Progress page).
 *
 * <p>FE sẽ hook {@code useStudyTimer} để gọi POST này mỗi ~30s khi user
 * còn tương tác và tab đang là "leader" (được chọn qua Redis).</p>
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Study.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Study (Theo dõi thời gian học)", description = "API ghi nhận heartbeat thời gian học cho trang Tiến độ")
public class StudyHeartbeatController {

    StudyHeartbeatService heartbeatService;
    CurrentUserService currentUserService;

    @Operation(
            summary = "Gửi heartbeat thời gian học",
            description = """
                    FE gọi mỗi ~30s khi user đang tương tác và tab hiện tại là "leader".

                    BE dùng Redis lock với key `study:hb:{userId}` (TTL 90s) để chỉ cho phép
                    1 tab trong 1 user ghi nhận thời gian — các tab khác nhận `accepted=false`.

                    Trường hợp Redis fail → vẫn ghi nhận (best-effort).""")
    @PostMapping(ApiPaths.Study.HEARTBEAT)
    public ApiResponse<StudyHeartbeatResponse> heartbeat(@Valid @RequestBody StudyHeartbeatRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        StudyHeartbeatResponse data = heartbeatService.record(userId, request);
        return ApiResponse.<StudyHeartbeatResponse>builder()
                .success(true)
                .data(data)
                .build();
    }
}