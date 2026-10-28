package com.jp.elearningjp.controller.speaking;

import com.jp.elearningjp.dto.request.speaking.CreateSessionRequest;
import com.jp.elearningjp.dto.request.speaking.SendMessageRequest;
import com.jp.elearningjp.dto.response.speaking.AiSessionResponse;
import com.jp.elearningjp.dto.response.speaking.AiSpeakingResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.speaking.AiSpeakingService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AiSpeaking.BASE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Speaking Practice", description = "APIs tạo phiên, gửi tin nhắn SSE stream và quản lý lịch sử luyện nói")
public class AiSpeakingController {

    private final AiSpeakingService aiSpeakingService;
    private final CurrentUserService currentUserService;

    private Long resolveCurrentUserId() {
        User currentUser = currentUserService.getCurrentUserOrNull();
        return (currentUser != null) ? currentUser.getId() : 1L;
    }

    @PostMapping
    @Operation(summary = "Tạo phiên luyện nói mới (Kịch bản hoặc Tự do)")
    public ApiResponse<AiSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        Long currentUserId = resolveCurrentUserId();
        AiSessionResponse response = aiSpeakingService.createSession(currentUserId, request);

        return ApiResponse.<AiSessionResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Tạo phiên luyện nói thành công")
                .data(response)
                .build();
    }

    @PostMapping(value = ApiPaths.AiSpeaking.MESSAGES, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Gửi câu thoại luyện nói và nhận phản hồi streaming qua Server-Sent Events (SSE)")
    public SseEmitter sendMessageStream(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {

        Long currentUserId = resolveCurrentUserId();
        // Timeout 3 phút cho luồng SSE
        SseEmitter emitter = new SseEmitter(180_000L);

        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for session {}", id);
            emitter.complete();
        });

        emitter.onError(throwable -> log.warn("SSE emitter error for session {}: {}", id, throwable.getMessage()));

        aiSpeakingService.sendMessageStream(currentUserId, id, request, emitter);
        return emitter;
    }

    @PostMapping(ApiPaths.AiSpeaking.MESSAGES + "/sync")
    @Operation(summary = "Gửi câu thoại đồng bộ (fallback không dùng SSE)")
    public ApiResponse<AiSpeakingResponse> sendMessageSync(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {

        Long currentUserId = resolveCurrentUserId();
        AiSpeakingResponse response = aiSpeakingService.sendMessageSync(currentUserId, id, request);

        return ApiResponse.<AiSpeakingResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Phản hồi thành công")
                .data(response)
                .build();
    }

    @GetMapping(ApiPaths.AiSpeaking.BY_ID)
    @Operation(summary = "Chi tiết phiên luyện nói và toàn bộ lịch sử tin nhắn")
    public ApiResponse<AiSessionResponse> getSession(@PathVariable Long id) {
        Long currentUserId = resolveCurrentUserId();
        AiSessionResponse response = aiSpeakingService.getSession(currentUserId, id);

        return ApiResponse.<AiSessionResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .build();
    }

    @GetMapping
    @Operation(summary = "Danh sách phiên luyện nói của người dùng (phân trang)")
    public ApiResponse<Page<AiSessionResponse>> getUserSessions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = resolveCurrentUserId();
        Page<AiSessionResponse> response = aiSpeakingService.getUserSessions(currentUserId, pageable);

        return ApiResponse.<Page<AiSessionResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .build();
    }

    @DeleteMapping(ApiPaths.AiSpeaking.BY_ID)
    @Operation(summary = "Xóa mềm một phiên luyện nói")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        Long currentUserId = resolveCurrentUserId();
        aiSpeakingService.deleteSession(currentUserId, id);

        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa phiên luyện nói thành công")
                .build();
    }

    @DeleteMapping
    @Operation(summary = "Xóa mềm toàn bộ lịch sử luyện nói của người dùng")
    public ApiResponse<Void> deleteAllSessions() {
        Long currentUserId = resolveCurrentUserId();
        aiSpeakingService.deleteAllSessions(currentUserId);

        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Đã dọn dẹp toàn bộ lịch sử luyện nói")
                .build();
    }
}
