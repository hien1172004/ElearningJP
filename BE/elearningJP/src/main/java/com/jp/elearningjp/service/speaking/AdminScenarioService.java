package com.jp.elearningjp.service.speaking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.dto.request.speaking.AdminScenarioRequest;
import com.jp.elearningjp.dto.response.speaking.AdminScenarioResponse;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.speaking.AiScenarioRepository;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Quản lý kịch bản luyện nói từ trang quản trị.
 * ADMIN: mọi kịch bản (kể cả kịch bản hệ thống và kịch bản cộng đồng);
 * TEACHER: chỉ kịch bản do chính mình tạo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminScenarioService {

    static final int MAX_PAGE_SIZE = 100;

    AiScenarioRepository scenarioRepository;
    CurrentUserService currentUserService;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<AdminScenarioResponse> getScenarios(String keyword, String topicId, JlptLevel jlptLevel,
                                                            int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        // TEACHER chỉ thấy kịch bản của mình; ADMIN thấy tất cả
        Long ownerFilter = currentUserService.isAdmin() ? null : currentUserService.getCurrentUserId();

        Page<AiScenario> result = scenarioRepository.findAll(
                buildSpec(keyword, topicId, jlptLevel, ownerFilter),
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.<AdminScenarioResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(safePage)
                .size(safeSize)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminScenarioResponse getScenario(Long id) {
        return toResponse(findManageable(id));
    }

    @Transactional
    public AdminScenarioResponse createScenario(AdminScenarioRequest request) {
        AiScenario scenario = new AiScenario();
        applyRequest(scenario, request);
        scenario.setCreatedBy(currentUserService.getCurrentUser());
        scenario = scenarioRepository.save(scenario);
        log.info("User {} created scenario {}", currentUserService.getCurrentUserId(), scenario.getId());
        return toResponse(scenario);
    }

    @Transactional
    public AdminScenarioResponse updateScenario(Long id, AdminScenarioRequest request) {
        AiScenario scenario = findManageable(id);
        applyRequest(scenario, request);
        scenario = scenarioRepository.save(scenario);
        log.info("User {} updated scenario {}", currentUserService.getCurrentUserId(), id);
        return toResponse(scenario);
    }

    /**
     * Xóa hẳn kịch bản. Các phiên luyện nói đã dùng kịch bản này vẫn giữ lại
     * (ai_session.scenario_id ON DELETE SET NULL).
     */
    @Transactional
    public void deleteScenario(Long id) {
        AiScenario scenario = findManageable(id);
        scenarioRepository.delete(scenario);
        log.info("User {} deleted scenario {}", currentUserService.getCurrentUserId(), id);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private AiScenario findManageable(Long id) {
        AiScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SCENARIO_NOT_FOUND));
        if (!currentUserService.isAdmin()) {
            User owner = scenario.getCreatedBy();
            if (owner == null || !Objects.equals(owner.getId(), currentUserService.getCurrentUserId())) {
                throw new AppException(ErrorCode.AI_SCENARIO_FORBIDDEN);
            }
        }
        return scenario;
    }

    private void applyRequest(AiScenario s, AdminScenarioRequest r) {
        s.setTopicId(r.getTopicId().trim());
        s.setJlptLevel(r.getJlptLevel());
        s.setTitle(r.getTitle().trim());
        s.setDescription(r.getDescription().trim());
        s.setUserRole(r.getUserRole().trim());
        s.setAiRole(r.getAiRole().trim());
        s.setTasks(buildTasks(r.getTasks()));
        s.setSampleDialogue(buildDialogue(r.getSampleDialogue()));
        s.setVocabulary(buildVocabulary(r.getVocabulary()));
        s.setGrammar(buildGrammar(r.getGrammar()));
        s.setTips(buildTips(r.getTips()));
        s.setPremium(Boolean.TRUE.equals(r.getIsPremium()));
        if (r.getIsLessonOnly() != null) {
            s.setLessonOnly(r.getIsLessonOnly());
        }
        s.setThumbnailUrl(blankToNull(r.getThumbnailUrl()));
    }

    /** id nhiệm vụ = vị trí (0..n-1) — phiên luyện nói lưu completedTasks theo id này. */
    private ArrayNode buildTasks(List<AdminScenarioRequest.TaskItem> items) {
        ArrayNode arr = objectMapper.createArrayNode();
        int i = 0;
        for (AdminScenarioRequest.TaskItem t : nullSafe(items)) {
            arr.add(objectMapper.createObjectNode().put("id", i++).put("text", t.getText().trim()));
        }
        return arr;
    }

    private ArrayNode buildDialogue(List<AdminScenarioRequest.DialogueLine> items) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (AdminScenarioRequest.DialogueLine d : nullSafe(items)) {
            ObjectNode n = objectMapper.createObjectNode()
                    .put("speaker", d.getSpeaker().trim().toUpperCase(Locale.ROOT))
                    .put("text", d.getText().trim());
            putIfPresent(n, "vi", d.getVi());
            arr.add(n);
        }
        return arr;
    }

    private ArrayNode buildVocabulary(List<AdminScenarioRequest.VocabItem> items) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (AdminScenarioRequest.VocabItem v : nullSafe(items)) {
            ObjectNode n = objectMapper.createObjectNode().put("word", v.getWord().trim());
            putIfPresent(n, "reading", v.getReading());
            n.put("meaning", v.getMeaning().trim());
            arr.add(n);
        }
        return arr;
    }

    private ArrayNode buildGrammar(List<AdminScenarioRequest.GrammarItem> items) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (AdminScenarioRequest.GrammarItem g : nullSafe(items)) {
            ObjectNode n = objectMapper.createObjectNode()
                    .put("pattern", g.getPattern().trim())
                    .put("meaning", g.getMeaning().trim());
            putIfPresent(n, "example", g.getExample());
            putIfPresent(n, "exampleMeaning", g.getExampleMeaning());
            arr.add(n);
        }
        return arr;
    }

    private ArrayNode buildTips(List<String> tips) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (String tip : nullSafe(tips)) {
            arr.add(tip.trim());
        }
        return arr;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static void putIfPresent(ObjectNode node, String field, String value) {
        String v = blankToNull(value);
        if (v != null) node.put(field, v);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static Specification<AiScenario> buildSpec(String keyword, String topicId, JlptLevel level, Long ownerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), ownerId));
            }
            if (topicId != null && !topicId.isBlank()) {
                predicates.add(cb.equal(root.get("topicId"), topicId.trim()));
            }
            if (level != null) {
                predicates.add(cb.equal(root.get("jlptLevel"), level));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminScenarioResponse toResponse(AiScenario s) {
        User owner = s.getCreatedBy();
        return AdminScenarioResponse.builder()
                .id(s.getId())
                .topicId(s.getTopicId())
                .jlptLevel(s.getJlptLevel())
                .title(s.getTitle())
                .description(s.getDescription())
                .userRole(s.getUserRole())
                .aiRole(s.getAiRole())
                .tasks(s.getTasks())
                .sampleDialogue(s.getSampleDialogue())
                .vocabulary(s.getVocabulary())
                .grammar(s.getGrammar())
                .tips(s.getTips())
                .isPremium(s.isPremium())
                .isLessonOnly(s.isLessonOnly())
                .thumbnailUrl(s.getThumbnailUrl())
                .createdById(owner != null ? owner.getId() : null)
                .createdByName(owner != null ? owner.getFullName() : null)
                .createdByEmail(owner != null ? owner.getEmail() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
