package com.jp.elearningjp.service.exam.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamPublishedVersion;
import com.jp.elearningjp.entity.exam.ExamSection;
import com.jp.elearningjp.entity.exam.ExamSectionQuestion;
import com.jp.elearningjp.entity.exam.Question;
import com.jp.elearningjp.entity.exam.QuestionOption;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.exam.ExamPublishedVersionRepository;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import com.jp.elearningjp.service.curriculum.snapshot.SnapshotJsonCache;
import com.jp.elearningjp.service.exam.impl.ExamViewBuilder;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Snapshot đề thi gắn khóa học: học viên làm / chấm / xem lại theo bản đã duyệt,
 * giáo viên sửa đề trên bản đang soạn (theo cùng luồng duyệt của khóa).
 * Đề độc lập (không gắn khóa) không dùng snapshot.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamSnapshotService {

    private final ExamRepository examRepository;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;
    private final ExamPublishedVersionRepository versionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamMapper examMapper;
    private final ExamViewBuilder examViewBuilder;
    private final CurrentUserService currentUserService;
    private final CourseAccessGuard courseAccessGuard;
    private final ObjectMapper objectMapper;
    private final SnapshotJsonCache snapshotCache;

    // ============================================================
    // TẠO SNAPSHOT
    // ============================================================

    /**
     * Khóa được duyệt: chụp lại mọi đề (chưa xóa) của khóa. Đề lần đầu được duyệt thì hiện cho học viên;
     * đề ADMIN đang ẩn vẫn ẩn (chỉ cập nhật bản chụp); đề giáo viên yêu cầu gỡ thì bị xóa mềm.
     */
    @Transactional
    public void publishCourseExams(Long courseId, User publishedBy) {
        for (Exam exam : examRepository.findByCourseIdAndDeletedFalse(courseId)) {
            // Giáo viên đã yêu cầu gỡ → lần duyệt này gỡ đề khỏi học viên: ẩn ngay (không ai bắt đầu lượt mới);
            // còn học viên đang làm dở thì chưa xóa để họ nộp được bài — lần duyệt sau xóa nốt
            if (exam.isPendingRemoval()) {
                exam.setPublished(false);
                long inProgress = examAttemptRepository.countByExamIdAndStatusAndDeletedFalse(
                        exam.getId(), AttemptStatus.IN_PROGRESS);
                if (inProgress == 0) {
                    exam.setPendingRemoval(false);
                    exam.softDelete();
                }
                examRepository.save(exam);
                log.info("[EXAM-SNAPSHOT] Exam id={} {} on course approval", exam.getId(),
                        inProgress == 0 ? "removed" : "hidden (" + inProgress + " attempt(s) in progress, delete next approval)");
                continue;
            }
            boolean firstApproval = exam.getPublishedVersionId() == null;
            publish(exam.getId(), publishedBy);
            if (firstApproval) {
                exam.setPublished(true);
                examRepository.save(exam);
            }
        }
    }

    /**
     * Tạo phiên bản mới cho đề thi (cả gắn khóa lẫn độc lập) từ bản đang soạn.
     * Nội dung không đổi so với phiên bản hiện tại thì giữ nguyên phiên bản cũ.
     */
    @Transactional
    public ExamPublishedVersion publish(Long examId, User publishedBy) {
        return publish(examId, publishedBy, null);
    }

    @Transactional
    public ExamPublishedVersion publish(Long examId, User publishedBy, String changelog) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        String content = write(build(exam));
        if (exam.getPublishedVersionId() != null) {
            Optional<ExamPublishedVersion> current = versionRepository.findById(exam.getPublishedVersionId());
            if (current.isPresent() && sameJson(current.get().getContent(), content)) {
                if (changelog != null && !changelog.isBlank() && current.get().getChangelog() == null) {
                    current.get().setChangelog(changelog);
                    versionRepository.save(current.get());
                }
                return current.get();
            }
        }

        ExamPublishedVersion version = versionRepository.save(ExamPublishedVersion.builder()
                .examId(examId)
                .versionNo(versionRepository.findMaxVersionNo(examId) + 1)
                .content(content)
                .publishedBy(publishedBy)
                .changelog(changelog)
                .build());
        exam.setPublishedVersionId(version.getId());
        examRepository.save(exam);
        log.info("[EXAM-SNAPSHOT] Exam id={} published version no={} (id={})",
                examId, version.getVersionNo(), version.getId());
        return version;
    }

    /** Bản đang soạn của đề có khác bản đã phát hành gần nhất không (true nếu chưa có bản phát hành). */
    @Transactional(readOnly = true)
    public boolean differsFromPublished(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
        if (exam.getPublishedVersionId() == null) {
            return true;
        }
        Optional<ExamPublishedVersion> current = versionRepository.findById(exam.getPublishedVersionId());
        if (current.isEmpty()) {
            return true;
        }
        String currentContent = current.get().getContent();
        String newContent = write(build(exam));
        boolean same = sameJson(currentContent, newContent);
        if (!same) {
            logDiff(examId, currentContent, newContent);
        }
        return !same;
    }

    private void logDiff(Long examId, String a, String b) {
        try {
            com.fasterxml.jackson.databind.JsonNode n1 = objectMapper.readTree(a);
            com.fasterxml.jackson.databind.JsonNode n2 = objectMapper.readTree(b);
            findAndLogDiff(examId, "", n1, n2);
        } catch (Exception e) {
            log.warn("[EXAM-SNAPSHOT] Cannot compare diff for examId={}: {}", examId, e.getMessage());
        }
    }

    private void findAndLogDiff(Long examId, String path, com.fasterxml.jackson.databind.JsonNode n1, com.fasterxml.jackson.databind.JsonNode n2) {
        if (n1.isObject() && n2.isObject()) {
            java.util.Set<String> allKeys = new java.util.TreeSet<>();
            n1.fieldNames().forEachRemaining(allKeys::add);
            n2.fieldNames().forEachRemaining(allKeys::add);
            for (String key : allKeys) {
                if (!n1.has(key)) {
                    log.info("[EXAM-SNAPSHOT-DIFF] examId={} {}.{} only in current snapshot", examId, path, key);
                } else if (!n2.has(key)) {
                    log.info("[EXAM-SNAPSHOT-DIFF] examId={} {}.{} only in draft", examId, path, key);
                } else {
                    findAndLogDiff(examId, path + "." + key, n1.get(key), n2.get(key));
                }
            }
        } else if (n1.isArray() && n2.isArray()) {
            if (n1.size() != n2.size()) {
                log.info("[EXAM-SNAPSHOT-DIFF] examId={} {} array size diff: {} vs {}", examId, path, n1.size(), n2.size());
            } else {
                for (int i = 0; i < n1.size(); i++) {
                    if (!n1.get(i).equals(n2.get(i))) {
                        findAndLogDiff(examId, path + "[" + i + "]", n1.get(i), n2.get(i));
                    }
                }
            }
        } else if (!n1.equals(n2)) {
            log.info("[EXAM-SNAPSHOT-DIFF] examId={} {} differs: current={} vs draft={}", examId, path, n1, n2);
        }
    }

    ExamSnapshot build(Exam exam) {
        List<ExamSection> sections = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toMap(ExamSection::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(ExamSection::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ExamSnapshot.Section> snapshotSections = sections.stream()
                .map(s -> ExamSnapshot.Section.builder()
                        .id(s.getId())
                        .sessionType(s.getSessionType())
                        .sectionTitle(s.getSectionTitle())
                        .audioUrl(s.getAudioUrl())
                        .orderIndex(s.getOrderIndex())
                        .durationMinutes(s.getDurationMinutes())
                        .scoreGroupId(s.getScoreGroup() != null ? s.getScoreGroup().getId() : null)
                        .questions(new java.util.ArrayList<>(examSectionQuestionRepository
                                .findBySectionIdWithQuestionDetails(s.getId()).stream()
                                .map(this::toSnapshotQuestion)
                                .toList()))
                        .build())
                .toList();

        ExamSummaryResponse summary = examMapper.toSummaryResponse(exam);
        // Không lưu id phiên bản / cờ ẩn-hiện vào nội dung (lúc đọc lấy theo đề): nếu không,
        // mỗi lần chụp đều "khác" bản trước dù câu hỏi không đổi
        summary.setPublishedVersionId(null);
        summary.setPublished(false);
        summary.setPendingRemoval(false);
        var detail = examViewBuilder.buildExamDetail(exam);
        detail.setPublished(false);
        return ExamSnapshot.builder()
                .summary(summary)
                .detail(detail)
                .sections(new java.util.ArrayList<>(snapshotSections))
                .build();
    }

    private ExamSnapshot.SectionQuestion toSnapshotQuestion(ExamSectionQuestion sq) {
        Question q = sq.getQuestion();
        List<QuestionOption> options = q.getOptions().stream().filter(o -> !o.isDeleted()).toList();
        return ExamSnapshot.SectionQuestion.builder()
                .questionId(q.getId())
                .questionNumber(sq.getQuestionNumber())
                .points(sq.getPoints())
                .skillType(q.getSkillType())
                .taking(examViewBuilder.toTakingQuestionResponse(q, sq.getQuestionNumber()))
                .review(examViewBuilder.toReviewQuestionResponse(q, sq.getQuestionNumber(), null))
                .optionIds(options.stream().map(QuestionOption::getId).sorted().collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .correctOptionIds(options.stream().filter(QuestionOption::isCorrect)
                        .map(QuestionOption::getId).sorted().collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .build();
    }

    // ============================================================
    // ĐỌC SNAPSHOT
    // ============================================================

    /**
     * Người dùng hiện tại làm đề theo snapshot: đề gắn khóa đã có snapshot và người dùng
     * không phải ADMIN / giáo viên được giao (họ thử đề trên bản đang soạn).
     */
    public boolean usesSnapshot(Exam exam) {
        return usesSnapshot(exam, currentUserService.getCurrentUserOrNull());
    }

    private boolean usesSnapshot(Exam exam, User user) {
        if (exam == null || exam.getPublishedVersionId() == null) {
            return false;
        }
        if (exam.getCourse() != null) {
            boolean manager = user != null
                    && (currentUserService.isAdmin() || courseAccessGuard.isAssignedTeacher(exam.getCourse(), user.getId()));
            return !manager;
        }
        // Đề độc lập/public đã xuất bản snapshot: tất cả thí sinh làm bài đều chạy trên snapshot bất biến
        return true;
    }

    public Optional<ExamSnapshot> find(Long publishedVersionId) {
        if (publishedVersionId == null) {
            return Optional.empty();
        }
        return snapshotCache.get("exam:" + publishedVersionId, ExamSnapshot.class,
                () -> versionRepository.findById(publishedVersionId).map(ExamPublishedVersion::getContent));
    }

    /** Tóm tắt đề theo snapshot, theo id phiên bản (1 query cho cả trang). */
    public Map<Long, ExamSummaryResponse> findSummaries(Collection<Long> publishedVersionIds) {
        if (publishedVersionIds == null || publishedVersionIds.isEmpty()) {
            return Map.of();
        }
        // Qua cache: thường đã có sẵn vì học viên vừa xem / làm đề
        Map<Long, ExamSummaryResponse> result = new java.util.HashMap<>();
        for (Long id : new java.util.LinkedHashSet<>(publishedVersionIds)) {
            find(id).map(ExamSnapshot::getSummary).ifPresent(s -> result.put(id, s));
        }
        return result;
    }

    /** Tóm tắt đề cho học viên: theo snapshot nếu có, không thì theo đề trực tiếp. */
    public List<ExamSummaryResponse> toLearnerSummaries(List<Exam> exams) {
        User user = currentUserService.getCurrentUserOrNull();
        List<Exam> fromSnapshot = exams.stream().filter(e -> usesSnapshot(e, user)).toList();
        java.util.Set<Long> snapshotExamIds = fromSnapshot.stream().map(Exam::getId).collect(Collectors.toSet());
        Map<Long, ExamSummaryResponse> summaries = findSummaries(
                fromSnapshot.stream().map(Exam::getPublishedVersionId).toList());

        Map<Long, ExamPublishedVersion> versionMap = versionRepository.findAllById(
                exams.stream().map(Exam::getPublishedVersionId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(ExamPublishedVersion::getId, Function.identity(), (a, b) -> a));

        return exams.stream()
                .map(e -> {
                    ExamSummaryResponse s = snapshotExamIds.contains(e.getId()) ? summaries.get(e.getPublishedVersionId()) : null;
                    if (s == null) {
                        s = examMapper.toSummaryResponse(e);
                    }
                    s.setPublished(e.isPublished());
                    if (e.getPublishedVersionId() != null && versionMap.containsKey(e.getPublishedVersionId())) {
                        ExamPublishedVersion v = versionMap.get(e.getPublishedVersionId());
                        s.setVersionNo(v.getVersionNo());
                        s.setChangelog(v.getChangelog());
                    }
                    return s;
                })
                .collect(Collectors.toList());
    }

    private String write(ExamSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("[EXAM-SNAPSHOT] Cannot serialize snapshot", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    private boolean sameJson(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        try {
            ExamSnapshot s1 = objectMapper.readValue(a, ExamSnapshot.class);
            ExamSnapshot s2 = objectMapper.readValue(b, ExamSnapshot.class);
            return java.util.Objects.equals(s1, s2);
        } catch (JsonProcessingException e) {
            try {
                return objectMapper.readTree(a).equals(objectMapper.readTree(b));
            } catch (JsonProcessingException ex) {
                return false;
            }
        }
    }
}
