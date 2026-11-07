package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.dto.request.curriculum.LessonSpeakingAssignRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonSpeakingScenario;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.speaking.AiSession;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSpeakingScenarioRepository;
import com.jp.elearningjp.repository.speaking.AiMessageRepository;
import com.jp.elearningjp.repository.speaking.AiScenarioRepository;
import com.jp.elearningjp.repository.speaking.AiSessionRepository;
import com.jp.elearningjp.shared.enums.AiMessageRole;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonSpeakingService {

    private final LessonSpeakingScenarioRepository lessonSpeakingRepo;
    private final LessonRepository lessonRepository;
    private final AiScenarioRepository scenarioRepository;
    private final AiSessionRepository sessionRepository;
    private final AiMessageRepository messageRepository;
    private final CourseAccessGuard courseAccessGuard;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey = "";

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel = "gemini-3.5-flash-lite";

    private final RestClient restClient = RestClient.builder().build();

    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-3.5-flash",
            "gemini-2.0-flash"
    );

    /**
     * Lấy danh sách tất cả các bài luyện nói của bài học.
     */
    @Transactional(readOnly = true)
    public List<LessonSpeakingResponse> getSpeakingListForLesson(Long lessonId, Long currentUserId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        List<LessonSpeakingScenario> list = lessonSpeakingRepo.findByLessonIdWithScenariosOrderByOrderIndexAsc(lessonId);
        return list.stream()
                .map(lss -> buildResponse(lss, currentUserId))
                .toList();
    }

    /**
     * Lấy kịch bản luyện nói đầu tiên của bài học (backward compatibility).
     */
    @Transactional(readOnly = true)
    public LessonSpeakingResponse getSpeakingForLesson(Long lessonId, Long currentUserId) {
        List<LessonSpeakingResponse> list = getSpeakingListForLesson(lessonId, currentUserId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Gán thêm hoặc cập nhật kịch bản luyện nói cho bài học (Dành cho Teacher / Admin).
     */
    @Transactional
    public LessonSpeakingResponse assignSpeakingScenario(Long lessonId, LessonSpeakingAssignRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Course course = lesson.getCourse();
        courseAccessGuard.requireOwnerOrAdmin(course);

        AiScenario scenario = scenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new AppException(ErrorCode.AI_SCENARIO_NOT_FOUND));

        int nextOrderIndex = request.getOrderIndex() != null && request.getOrderIndex() > 0
                ? request.getOrderIndex()
                : (int) lessonSpeakingRepo.countByLessonId(lessonId) + 1;

        LessonSpeakingScenario lessonSpeaking = lessonSpeakingRepo.findByLessonIdAndScenarioId(lessonId, request.getScenarioId())
                .orElseGet(() -> LessonSpeakingScenario.builder()
                        .lesson(lesson)
                        .scenario(scenario)
                        .build());

        lessonSpeaking.setScenario(scenario);
        lessonSpeaking.setOrderIndex(nextOrderIndex);
        if (request.getMinTurns() != null && request.getMinTurns() > 0) {
            lessonSpeaking.setMinTurns(request.getMinTurns());
        }

        LessonSpeakingScenario saved = lessonSpeakingRepo.save(lessonSpeaking);
        log.info("[LESSON_SPEAKING] Assigned scenario id={} to lesson id={} with orderIndex={} minTurns={}",
                scenario.getId(), lessonId, saved.getOrderIndex(), saved.getMinTurns());

        return buildResponse(saved, null);
    }

    /**
     * Xóa 1 kịch bản hoặc toàn bộ kịch bản luyện nói khỏi bài học (Dành cho Teacher / Admin).
     */
    @Transactional
    public void removeSpeakingScenario(Long lessonId, Long scenarioId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        courseAccessGuard.requireOwnerOrAdmin(lesson.getCourse());

        if (scenarioId != null) {
            lessonSpeakingRepo.deleteByLessonIdAndScenarioId(lessonId, scenarioId);
            log.info("[LESSON_SPEAKING] Removed scenario id={} from lesson id={}", scenarioId, lessonId);
        } else {
            lessonSpeakingRepo.deleteByLessonId(lessonId);
            log.info("[LESSON_SPEAKING] Removed all speaking scenarios from lesson id={}", lessonId);
        }
    }

    /**
     * AI tự động phân tích từ vựng và ngữ pháp của bài học để tạo ra kịch bản hội thoại chuẩn JLPT (N5 - N1).
     */
    @Transactional
    public LessonSpeakingResponse generateScenarioFromLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Course course = lesson.getCourse();
        courseAccessGuard.requireOwnerOrAdmin(course);

        // 1. Trích xuất từ vựng và ngữ pháp bài học
        List<String> vocabList = new ArrayList<>();
        List<String> grammarList = new ArrayList<>();
        if (lesson.getSections() != null) {
            for (LessonSection section : lesson.getSections()) {
                if (section.getVocabularyItems() != null) {
                    for (LessonVocabularyItem item : section.getVocabularyItems()) {
                        if (item.getVocab() != null) {
                            String word = item.getVocab().getWord();
                            if (item.getVocab().getMeaningVi() != null && !item.getVocab().getMeaningVi().isBlank()) {
                                word += " (" + item.getVocab().getMeaningVi() + ")";
                            }
                            vocabList.add(word);
                        }
                    }
                }
                if (section.getGrammarItems() != null) {
                    for (LessonGrammarItem item : section.getGrammarItems()) {
                        if (item.getGrammar() != null) {
                            String pattern = item.getGrammar().getTitle();
                            if (item.getCustomMeaning() != null && !item.getCustomMeaning().isBlank()) {
                                pattern += " (" + item.getCustomMeaning() + ")";
                            } else if (item.getGrammar().getMeaningVi() != null && !item.getGrammar().getMeaningVi().isBlank()) {
                                pattern += " (" + item.getGrammar().getMeaningVi() + ")";
                            }
                            grammarList.add(pattern);
                        }
                    }
                }
            }
        }

        String level = course.getJlptLevel() != null ? course.getJlptLevel().name() : "N5";
        String vocabStr = vocabList.isEmpty() ? "Hội thoại giao tiếp hàng ngày" : String.join(", ", vocabList);
        String grammarStr = grammarList.isEmpty() ? "Các mẫu câu lịch sự thông dụng" : String.join(", ", grammarList);

        // 2. Gọi Gemini sinh kịch bản JSON
        JsonNode scenarioJson = callGeminiToGenerateScenario(lesson.getTitle(), level, vocabStr, grammarStr);

        // 3. Xây dựng entity AiScenario
        String title = scenarioJson.path("title").asText("Hội thoại: " + lesson.getTitle());
        String description = scenarioJson.path("description").asText("Luyện tập hội thoại giao tiếp ứng dụng từ vựng và ngữ pháp bài học " + lesson.getTitle());
        String userRole = scenarioJson.path("userRole").asText("Người học");
        String aiRole = scenarioJson.path("aiRole").asText("Đối tác đàm thoại");

        ArrayNode tasksArray = objectMapper.createArrayNode();
        JsonNode rawTasks = scenarioJson.path("tasks");
        if (rawTasks.isArray() && !rawTasks.isEmpty()) {
            int idx = 0;
            for (JsonNode t : rawTasks) {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("id", idx++);
                item.put("text", t.isObject() ? t.path("text").asText() : t.asText());
                tasksArray.add(item);
            }
        } else {
            ObjectNode t1 = objectMapper.createObjectNode();
            t1.put("id", 0);
            t1.put("text", "Vận dụng từ vựng bài học để chào hỏi và nêu nhu cầu giao tiếp");
            ObjectNode t2 = objectMapper.createObjectNode();
            t2.put("id", 1);
            t2.put("text", "Ứng dụng ngữ pháp đã học để hoàn thành cuộc đối thoại");
            tasksArray.add(t1);
            tasksArray.add(t2);
        }

        JsonNode sampleDialogue = scenarioJson.path("sampleDialogue");
        if (!sampleDialogue.isArray() || sampleDialogue.isEmpty()) {
            ArrayNode sampleArray = objectMapper.createArrayNode();
            ObjectNode line1 = objectMapper.createObjectNode();
            line1.put("speaker", "AI");
            line1.put("text", "こんにちは！今日は何かお手伝いできますか？");
            line1.put("vi", "Xin chào! Hôm nay tôi có thể giúp gì cho bạn?");
            ObjectNode line2 = objectMapper.createObjectNode();
            line2.put("speaker", "USER");
            line2.put("text", "こんにちは、よろしくお願いします.");
            line2.put("vi", "Xin chào, rất mong được bạn giúp đỡ.");
            sampleArray.add(line1);
            sampleArray.add(line2);
            sampleDialogue = sampleArray;
        }

        JsonNode vocabNode = scenarioJson.path("vocabulary");
        if (!vocabNode.isArray()) {
            vocabNode = objectMapper.createArrayNode();
        }

        JsonNode grammarNode = scenarioJson.path("grammar");
        if (!grammarNode.isArray()) {
            grammarNode = objectMapper.createArrayNode();
        }

        JsonNode tipsNode = scenarioJson.path("tips");
        if (!tipsNode.isArray()) {
            ArrayNode tipsArray = objectMapper.createArrayNode();
            tipsArray.add("Hãy chú ý sử dụng đúng thể lịch sự và trợ từ khi trả lời.");
            tipsNode = tipsArray;
        }

        AiScenario scenario = AiScenario.builder()
                .title(title.length() > 255 ? title.substring(0, 255) : title)
                .description(description)
                .userRole(userRole.length() > 100 ? userRole.substring(0, 100) : userRole)
                .aiRole(aiRole.length() > 100 ? aiRole.substring(0, 100) : aiRole)
                .topicId("LESSON_PRACTICE")
                .jlptLevel(course.getJlptLevel() != null ? course.getJlptLevel() : JlptLevel.N5)
                .tasks(tasksArray)
                .sampleDialogue(sampleDialogue)
                .vocabulary(vocabNode)
                .grammar(grammarNode)
                .tips(tipsNode)
                .isPremium(false)
                .isLessonOnly(true)
                .build();

        AiScenario savedScenario = scenarioRepository.save(scenario);

        // 4. Gán kịch bản vào bài học theo thứ tự tiếp theo
        int currentCount = (int) lessonSpeakingRepo.countByLessonId(lessonId);
        int nextOrder = currentCount + 1;

        LessonSpeakingScenario lessonSpeaking = LessonSpeakingScenario.builder()
                .lesson(lesson)
                .scenario(savedScenario)
                .orderIndex(nextOrder)
                .minTurns(3)
                .build();

        LessonSpeakingScenario savedLss = lessonSpeakingRepo.save(lessonSpeaking);
        log.info("[LESSON_SPEAKING] AI auto-generated scenario id={} orderIndex={} for lesson id={}",
                savedScenario.getId(), nextOrder, lessonId);

        return buildResponse(savedLss, null);
    }

    private JsonNode callGeminiToGenerateScenario(String lessonTitle, String level, String vocabStr, String grammarStr) {
        String promptText = """
                Bạn là một chuyên gia sư phạm tiếng Nhật hàng đầu. Hãy thiết kế một kịch bản luyện nói nhập vai (Roleplay Speaking Scenario) thực tế, sống động bám sát nội dung bài học:
                - Tên bài học: %s
                - Trình độ JLPT: %s (N5, N4, N3, N2 hoặc N1)
                - Từ vựng trọng tâm bài học: %s
                - Ngữ pháp trọng tâm bài học: %s

                YÊU CẦU:
                Tạo một kịch bản tình huống đời sống/công việc mà người học cần sử dụng các từ vựng và ngữ pháp trên.
                Trả về DUY NHẤT một khối mã JSON hợp lệ theo cấu trúc sau (không viết thêm bất kỳ lời giải thích nào bên ngoài):
                {
                  "title": "Tên tình huống ngắn gọn hấp dẫn",
                  "description": "Bối cảnh 2-3 câu mô tả người học đang ở đâu, gặp ai và cần làm gì",
                  "userRole": "Vai của người học",
                  "aiRole": "Vai của AI",
                  "tasks": [
                    "Nhiệm vụ 1 người học cần hoàn thành trong cuộc trò chuyện",
                    "Nhiệm vụ 2...",
                    "Nhiệm vụ 3..."
                  ],
                  "sampleDialogue": [
                    {"speaker": "AI", "text": "Câu tiếng Nhật chuẩn tự nhiên", "vi": "Dịch nghĩa tiếng Việt"},
                    {"speaker": "USER", "text": "Câu tiếng Nhật người học trả lời", "vi": "Dịch nghĩa tiếng Việt"},
                    {"speaker": "AI", "text": "Câu tiếng Nhật tiếp theo của AI", "vi": "Dịch nghĩa tiếng Việt"},
                    {"speaker": "USER", "text": "Câu tiếng Nhật tiếp theo", "vi": "Dịch nghĩa tiếng Việt"}
                  ],
                  "vocabulary": [
                    {"word": "Từ vựng", "reading": "Cách đọc hiragana", "meaning": "Nghĩa tiếng Việt"}
                  ],
                  "tips": [
                    "Mẹo nói tự nhiên hoặc lưu ý khi hội thoại"
                  ]
                }
                """.formatted(lessonTitle, level, vocabStr, grammarStr);

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            List<String> models = new ArrayList<>();
            if (geminiModel != null && !geminiModel.isBlank()) {
                models.add(geminiModel.trim());
            }
            for (String m : FALLBACK_MODELS) {
                if (!models.contains(m)) models.add(m);
            }

            Map<String, Object> part = Map.of("text", promptText);
            Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
            Map<String, Object> body = Map.of("contents", List.of(content));

            for (String currentModel : models) {
                try {
                    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + geminiApiKey;
                    String raw = restClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(String.class);

                    if (raw != null) {
                        JsonNode root = objectMapper.readTree(raw);
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && !candidates.isEmpty()) {
                            JsonNode parts = candidates.get(0).path("content").path("parts");
                            if (parts.isArray() && !parts.isEmpty()) {
                                String text = parts.get(0).path("text").asText();
                                Matcher matcher = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```").matcher(text);
                                String jsonStr = matcher.find() ? matcher.group(1).trim() : text.trim();
                                return objectMapper.readTree(jsonStr);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Gemini model {} failed during scenario generation: {}. Trying fallback...", currentModel, e.getMessage());
                }
            }
        }

        // Fallback tự sinh kịch bản mặc định nếu Gemini API chưa cấu hình hoặc mất mạng
        log.warn("Gemini không phản hồi hoặc chưa cấu hình API key. Tự sinh kịch bản fallback theo bài học.");
        return buildFallbackScenarioJson(lessonTitle, level, vocabStr);
    }

    private JsonNode buildFallbackScenarioJson(String lessonTitle, String level, String vocabStr) {
        ObjectNode fallback = objectMapper.createObjectNode();
        fallback.put("title", "Hội thoại ứng dụng: " + lessonTitle);
        fallback.put("description", "Bạn đang tham gia buổi luyện nói giao tiếp tiếng Nhật trình độ " + level + " xoay quanh chủ đề " + lessonTitle + ". Hãy tự tin đối thoại cùng trợ lý AI!");
        fallback.put("userRole", "Học viên (" + level + ")");
        fallback.put("aiRole", "Người bản xứ / Đối tác đàm thoại");

        ArrayNode tasks = objectMapper.createArrayNode();
        tasks.add("Chào hỏi và giới thiệu chủ đề cuộc trò chuyện");
        tasks.add("Vận dụng từ vựng trọng tâm bài học để diễn đạt ý muốn");
        tasks.add("Lắng nghe phản hồi và kết thúc cuộc đối thoại một cách lịch sự");
        fallback.set("tasks", tasks);

        ArrayNode dialogue = objectMapper.createArrayNode();
        ObjectNode d1 = objectMapper.createObjectNode();
        d1.put("speaker", "AI");
        d1.put("text", "こんにちは！今日は「" + lessonTitle + "」について話しましょう。準備はいいですか？");
        d1.put("vi", "Xin chào! Hôm nay chúng ta hãy cùng nói về chủ đề " + lessonTitle + " nhé. Bạn đã sẵn sàng chưa?");
        ObjectNode d2 = objectMapper.createObjectNode();
        d2.put("speaker", "USER");
        d2.put("text", "はい、よろしくお願いします！頑張ります。");
        d2.put("vi", "Vâng, rất mong được bạn giúp đỡ! Tôi sẽ cố gắng.");
        ObjectNode d3 = objectMapper.createObjectNode();
        d3.put("speaker", "AI");
        d3.put("text", "素晴らしいですね。では、早速始めてみましょう！");
        d3.put("vi", "Tuyệt vời. Vậy chúng ta hãy bắt đầu ngay nhé!");
        dialogue.add(d1);
        dialogue.add(d2);
        dialogue.add(d3);
        fallback.set("sampleDialogue", dialogue);

        ArrayNode vocab = objectMapper.createArrayNode();
        fallback.set("vocabulary", vocab);

        ArrayNode tips = objectMapper.createArrayNode();
        tips.add("Nói to, rõ ràng và sử dụng đúng trợ từ trong câu.");
        tips.add("Nếu chưa kịp nghĩ ra từ mới, hãy sử dụng các từ đơn giản tương đương.");
        fallback.set("tips", tips);

        return fallback;
    }

    /**
     * Build DTO phản hồi, bao gồm thông tin session tích cực của user (nếu có).
     */
    public LessonSpeakingResponse buildResponse(LessonSpeakingScenario lss, Long currentUserId) {
        AiScenario sc = lss.getScenario();
        Long activeSessionId = null;
        int userTurnsCount = 0;
        boolean completed = false;

        if (currentUserId != null) {
            Optional<AiSession> sessionOpt = sessionRepository
                    .findFirstByLesson_IdAndScenario_IdAndUser_IdAndDeletedFalseOrderByCreatedAtDesc(
                            lss.getLesson().getId(), sc.getId(), currentUserId);

            if (sessionOpt.isPresent()) {
                AiSession session = sessionOpt.get();
                activeSessionId = session.getId();
                List<AiMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
                long count = messages.stream().filter(m -> m.getRole() == AiMessageRole.USER).count();
                userTurnsCount = (int) count;

                int totalTasks = (sc.getTasks() != null && sc.getTasks().isArray()) ? sc.getTasks().size() : 0;
                if (totalTasks > 0) {
                    int completedCount = (session.getCompletedTasks() != null && session.getCompletedTasks().isArray())
                            ? session.getCompletedTasks().size()
                            : 0;
                    completed = completedCount >= totalTasks;
                } else {
                    completed = userTurnsCount >= (lss.getMinTurns() != null ? lss.getMinTurns() : 3);
                }
            }
        }

        return LessonSpeakingResponse.builder()
                .id(lss.getId())
                .lessonId(lss.getLesson().getId())
                .scenarioId(sc.getId())
                .orderIndex(lss.getOrderIndex())
                .minTurns(lss.getMinTurns() != null ? lss.getMinTurns() : 3)
                .topicId(sc.getTopicId())
                .jlptLevel(sc.getJlptLevel())
                .title(sc.getTitle())
                .description(sc.getDescription())
                .userRole(sc.getUserRole())
                .aiRole(sc.getAiRole())
                .tasks(sc.getTasks())
                .sampleDialogue(sc.getSampleDialogue())
                .vocabulary(sc.getVocabulary())
                .grammar(sc.getGrammar())
                .tips(sc.getTips())
                .activeSessionId(activeSessionId)
                .userTurnsCount(userTurnsCount)
                .completed(completed)
                .build();
    }
}
