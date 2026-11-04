package com.jp.elearningjp.service.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.speaking.AdminScenarioRequest;
import com.jp.elearningjp.dto.response.speaking.AdminScenarioResponse;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.speaking.AiScenarioRepository;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminScenarioServiceTest {

    private static final Long TEACHER_ID = 10L;
    private static final Long OTHER_ID = 11L;

    private AiScenarioRepository repository;
    private CurrentUserService currentUserService;
    private AdminScenarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(AiScenarioRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new AdminScenarioService(repository, currentUserService, new ObjectMapper());
        when(repository.save(any(AiScenario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void loginAsTeacher() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(TEACHER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user(TEACHER_ID));
    }

    private void loginAsAdmin() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user(1L));
    }

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.com").fullName("User " + id).build();
        u.setId(id);
        return u;
    }

    private static AiScenario scenario(Long id, Long ownerId) {
        AiScenario s = new AiScenario();
        s.setId(id);
        s.setTitle("S" + id);
        s.setCreatedBy(ownerId == null ? null : user(ownerId));
        return s;
    }

    private static AdminScenarioRequest request() {
        return AdminScenarioRequest.builder()
                .topicId("DINING_SHOPPING")
                .jlptLevel(JlptLevel.N5)
                .title("  Gọi món  ")
                .description("Mô tả")
                .userRole("Khách")
                .aiRole("Nhân viên")
                .tasks(List.of(new AdminScenarioRequest.TaskItem("Chào"), new AdminScenarioRequest.TaskItem("Gọi món")))
                .sampleDialogue(List.of(
                        new AdminScenarioRequest.DialogueLine("ai", "いらっしゃいませ", " "),
                        new AdminScenarioRequest.DialogueLine("USER", "ラーメンをください", "Cho tôi ramen")))
                .vocabulary(List.of(new AdminScenarioRequest.VocabItem("注文", "", "Gọi món")))
                .grammar(List.of(new AdminScenarioRequest.GrammarItem("〜をください", "Cho tôi ~", null, null)))
                .tips(List.of(" Nói chậm "))
                .thumbnailUrl("")
                .build();
    }

    // ============================================================
    // Phân quyền theo người tạo
    // ============================================================

    @Test
    void teacher_cannotManageScenarioOfAnotherUser() {
        loginAsTeacher();
        when(repository.findById(5L)).thenReturn(Optional.of(scenario(5L, OTHER_ID)));

        assertThatThrownBy(() -> service.updateScenario(5L, request()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SCENARIO_FORBIDDEN);
        assertThatThrownBy(() -> service.deleteScenario(5L))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SCENARIO_FORBIDDEN);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(AiScenario.class));
    }

    @Test
    void teacher_cannotManageSystemOrGuestScenario() {
        loginAsTeacher();
        when(repository.findById(6L)).thenReturn(Optional.of(scenario(6L, null)));

        assertThatThrownBy(() -> service.getScenario(6L))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SCENARIO_FORBIDDEN);
    }

    @Test
    void teacher_canManageOwnScenario() {
        loginAsTeacher();
        AiScenario own = scenario(7L, TEACHER_ID);
        when(repository.findById(7L)).thenReturn(Optional.of(own));

        service.updateScenario(7L, request());
        service.deleteScenario(7L);

        verify(repository).save(own);
        verify(repository).delete(own);
    }

    @Test
    void admin_canManageAnyScenario_includingSystemOnes() {
        loginAsAdmin();
        AiScenario system = scenario(8L, null);
        when(repository.findById(8L)).thenReturn(Optional.of(system));

        service.deleteScenario(8L);

        verify(repository).delete(system);
    }

    @Test
    void missingScenario_throwsNotFound() {
        loginAsAdmin();
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getScenario(99L))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SCENARIO_NOT_FOUND);
    }

    // ============================================================
    // Danh sách
    // ============================================================

    @Test
    @SuppressWarnings("unchecked")
    void list_clampsPaging_andReturnsCreatorInfo() {
        loginAsAdmin();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(scenario(1L, TEACHER_ID), scenario(2L, null)), inv.getArgument(1), 2));

        var res = service.getScenarios(null, null, null, 0, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(AdminScenarioService.MAX_PAGE_SIZE);
        assertThat(res.getItems()).extracting(AdminScenarioResponse::getCreatedById).containsExactly(TEACHER_ID, null);
    }

    // ============================================================
    // Tạo mới: người tạo + chuyển request thành JSON đúng cấu trúc
    // ============================================================

    @Test
    void create_setsCurrentUserAsOwner_andBuildsJsonLikeSeedData() {
        loginAsTeacher();

        AdminScenarioResponse res = service.createScenario(request());

        assertThat(res.getCreatedById()).isEqualTo(TEACHER_ID);
        assertThat(res.getTitle()).isEqualTo("Gọi món");
        assertThat(res.getThumbnailUrl()).isNull();
        assertThat(res.getIsPremium()).isFalse();

        JsonNode tasks = res.getTasks();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).path("id").asInt()).isZero();
        assertThat(tasks.get(1).path("id").asInt()).isEqualTo(1);
        assertThat(tasks.get(1).path("text").asText()).isEqualTo("Gọi món");

        JsonNode dialogue = res.getSampleDialogue();
        assertThat(dialogue.get(0).path("speaker").asText()).isEqualTo("AI");
        assertThat(dialogue.get(0).has("vi")).isFalse(); // bản dịch trống không được lưu
        assertThat(dialogue.get(1).path("vi").asText()).isEqualTo("Cho tôi ramen");

        assertThat(res.getVocabulary().get(0).has("reading")).isFalse();
        assertThat(res.getGrammar().get(0).has("example")).isFalse();
        assertThat(res.getTips().get(0).asText()).isEqualTo("Nói chậm");
    }
}
