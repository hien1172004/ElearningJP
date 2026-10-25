package com.jp.elearningjp.service.exam.impl;

import com.jp.elearningjp.dto.request.exam.CreateQuestionRequest;
import com.jp.elearningjp.dto.request.exam.ImportExamRequest;
import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.*;
import com.jp.elearningjp.entity.content.ReadingPassage;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.exam.*;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.content.ReadingPassageRepository;
import com.jp.elearningjp.repository.exam.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.exam.AdminExamService;
import com.jp.elearningjp.shared.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminExamServiceImpl implements AdminExamService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final MondaiTypeRepository mondaiTypeRepository;
    private final ReadingPassageRepository readingPassageRepository;
    private final UserRepository userRepository;
    private final com.jp.elearningjp.repository.curriculum.CourseRepository courseRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ExamMapper examMapper;

    @Override
    public Long importExam(ImportExamRequest request, Long currentUserId) {
        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        int overallPassingScore = 80;
        if (request.getJlptLevel() != null) {
            switch (request.getJlptLevel()) {
                case N1 -> overallPassingScore = 100;
                case N2 -> overallPassingScore = 90;
                case N3 -> overallPassingScore = 95;
                case N4 -> overallPassingScore = 90;
                case N5 -> overallPassingScore = 80;
            }
        }

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId()).orElse(null);
        }

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .jlptLevel(request.getJlptLevel())
                .examType(request.getExamType() != null ? request.getExamType() : ExamType.JLPT_OFFICIAL_MOCK)
                .totalDurationMinutes(request.getTotalDurationMinutes() != null ? request.getTotalDurationMinutes() : 140)
                .totalScaledMaxScore(180)
                .overallPassingScore(overallPassingScore)
                .sourceFileName(request.getSourceFileName())
                .published(false)
                .createdBy(creator)
                .course(course)
                .build();

        ExamScoreGroup knowledgeGroup;
        ExamScoreGroup readingGroup = null;
        ExamScoreGroup listeningGroup;

        if (request.getJlptLevel() == JlptLevel.N5 || request.getJlptLevel() == JlptLevel.N4) {
            knowledgeGroup = ExamScoreGroup.builder()
                    .groupCode(ScoreGroupCode.LANGUAGE_KNOWLEDGE_READING)
                    .maxScaledScore(120)
                    .minRequiredScore(38)
                    .build();
            listeningGroup = ExamScoreGroup.builder()
                    .groupCode(ScoreGroupCode.LISTENING)
                    .maxScaledScore(60)
                    .minRequiredScore(19)
                    .build();
            exam.addScoreGroup(knowledgeGroup);
            exam.addScoreGroup(listeningGroup);
        } else {
            knowledgeGroup = ExamScoreGroup.builder()
                    .groupCode(ScoreGroupCode.LANGUAGE_KNOWLEDGE)
                    .maxScaledScore(60)
                    .minRequiredScore(19)
                    .build();
            readingGroup = ExamScoreGroup.builder()
                    .groupCode(ScoreGroupCode.READING)
                    .maxScaledScore(60)
                    .minRequiredScore(19)
                    .build();
            listeningGroup = ExamScoreGroup.builder()
                    .groupCode(ScoreGroupCode.LISTENING)
                    .maxScaledScore(60)
                    .minRequiredScore(19)
                    .build();
            exam.addScoreGroup(knowledgeGroup);
            exam.addScoreGroup(readingGroup);
            exam.addScoreGroup(listeningGroup);
        }

        exam = examRepository.save(exam);

        int totalQuestions = 0;
        int sectionOrder = 1;

        if (request.getSections() != null) {
            for (var secReq : request.getSections()) {
                ExamScoreGroup targetGroup = switch (secReq.getSessionType()) {
                    case LISTENING -> listeningGroup;
                    case GRAMMAR_READING -> (readingGroup != null ? readingGroup : knowledgeGroup);
                    case KNOWLEDGE_READING -> knowledgeGroup;
                    default -> knowledgeGroup;
                };

                ExamSection section = ExamSection.builder()
                        .exam(exam)
                        .scoreGroup(targetGroup)
                        .sessionType(secReq.getSessionType())
                        .sectionTitle(secReq.getSectionTitle())
                        .audioUrl(secReq.getAudioUrl())
                        .orderIndex(secReq.getOrderIndex() != null ? secReq.getOrderIndex() : sectionOrder++)
                        .durationMinutes(secReq.getDurationMinutes())
                        .build();

                exam.addSection(section);
                section = examSectionRepository.save(section);

                int qNum = 1;
                java.util.Map<String, ReadingPassage> sectionPassageCache = new java.util.HashMap<>();
                if (secReq.getQuestions() != null) {
                    for (var qReq : secReq.getQuestions()) {
                        totalQuestions++;

                        ReadingPassage passage = null;
                        if (qReq.getPassage() != null && qReq.getPassage().getContentJp() != null && !qReq.getPassage().getContentJp().isBlank()) {
                            String pKey = qReq.getPassage().getContentJp().trim();
                            if (sectionPassageCache.containsKey(pKey)) {
                                passage = sectionPassageCache.get(pKey);
                            } else {
                                passage = ReadingPassage.builder()
                                        .title(qReq.getPassage().getTitle() != null ? qReq.getPassage().getTitle() : "Bài đọc")
                                        .jlptLevel(request.getJlptLevel())
                                        .contentJp(qReq.getPassage().getContentJp())
                                        .passageType(qReq.getPassage().getPassageType() != null ? qReq.getPassage().getPassageType() : PassageType.TEXT)
                                        .imageUrl(qReq.getPassage().getImageUrl())
                                        .active(true)
                                        .build();
                                passage = readingPassageRepository.save(passage);
                                sectionPassageCache.put(pKey, passage);
                            }
                        }

                        MondaiType mondaiType = null;
                        if (qReq.getMondaiCode() != null) {
                            mondaiType = mondaiTypeRepository.findByCodeAndDeletedFalse(qReq.getMondaiCode()).orElse(null);
                        }
                        if (mondaiType == null) {
                            SkillType targetSkill = switch (secReq.getSessionType()) {
                                case LISTENING -> SkillType.LISTENING;
                                case GRAMMAR_READING -> SkillType.READING;
                                case KNOWLEDGE_READING -> (qReq.getPassage() != null ? SkillType.READING : SkillType.VOCABULARY);
                                default -> SkillType.VOCABULARY;
                            };
                            mondaiType = mondaiTypeRepository.findBySkillTypeAndActiveTrueAndDeletedFalse(targetSkill)
                                    .stream().findFirst().orElseGet(() ->
                                            mondaiTypeRepository.findAll().stream().findFirst().orElse(null)
                                    );
                        }

                        Question question = new Question();
                        question.setJlptLevel(request.getJlptLevel());
                        question.setSkillType(mondaiType != null ? mondaiType.getSkillType() : SkillType.VOCABULARY);
                        question.setMondaiType(mondaiType);
                        question.setPassage(passage);
                        question.setQuestionText(qReq.getQuestionText());
                        question.setImageUrl(qReq.getImageUrl());
                        question.setAudioUrl(secReq.getAudioUrl());
                        question.setAudioStartTime(qReq.getAudioStartTime());
                        question.setAudioEndTime(qReq.getAudioEndTime());
                        question.setTranscriptJp(qReq.getTranscriptJp());
                        question.setTranscriptVi(qReq.getTranscriptVi());
                        question.setStarPosition(qReq.getStarPosition());
                        question.setHasVisualOptions(Boolean.TRUE.equals(qReq.getHasVisualOptions()));
                        question.setExplanationVi(qReq.getExplanationVi());
                        question.setActive(true);
                        question.setCreatedBy(creator);

                        short optKey = 1;
                        if (qReq.getOptions() != null) {
                            // Rule: Mỗi câu trắc nghiệm JLPT chỉ có DUY NHẤT 1 đáp án đúng
                            Integer targetCorrectKey = null;
                            for (var optReq : qReq.getOptions()) {
                                if (Boolean.TRUE.equals(optReq.getIsCorrect())) {
                                    if (targetCorrectKey == null || (optReq.getOptionKey() != null && optReq.getOptionKey() > 1)) {
                                        targetCorrectKey = optReq.getOptionKey() != null ? optReq.getOptionKey().intValue() : 1;
                                    }
                                }
                            }
                            if (targetCorrectKey == null) {
                                targetCorrectKey = 1;
                            }

                            for (var optReq : qReq.getOptions()) {
                                short currentKey = optReq.getOptionKey() != null ? optReq.getOptionKey() : optKey++;
                                boolean isCorrect = (currentKey == targetCorrectKey.shortValue());
                                QuestionOption option = QuestionOption.builder()
                                        .optionKey(currentKey)
                                        .optionText(optReq.getOptionText())
                                        .optionImageUrl(optReq.getOptionImageUrl())
                                        .correct(isCorrect)
                                        .build();
                                question.addOption(option);
                            }
                        }

                        question = questionRepository.save(question);

                        ExamSectionQuestion esq = ExamSectionQuestion.builder()
                                .exam(exam)
                                .section(section)
                                .question(question)
                                .questionNumber(qNum++)
                                .points(new BigDecimal("1.00"))
                                .build();

                        examSectionQuestionRepository.save(esq);
                    }
                }
            }
        }

        exam.setTotalQuestions(totalQuestions);
        examRepository.save(exam);

        log.info("✅ Đã tạo thành công đề thi nháp ID: {}, tổng số câu hỏi: {}", exam.getId(), totalQuestions);
        return exam.getId();
    }

    @Override
    @Transactional
    public AdminExamPreviewResponse getExamPreview(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        var scoreGroups = exam.getScoreGroups().stream()
                .map(examMapper::toScoreGroupResponse)
                .collect(Collectors.toList());

        List<ReviewSectionResponse> reviewSections = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparingInt(ExamSection::getOrderIndex))
                .<ReviewSectionResponse>map(s -> {
                    var sectionQuestions = examSectionQuestionRepository
                            .findBySectionIdWithQuestionDetails(s.getId());

                    int currentSecNum = 1;
                    List<ReviewQuestionResponse> reviewQuestions = new ArrayList<>();
                    for (var sq : sectionQuestions) {
                        Question q = sq.getQuestion();
                        // Tự động chữa lành (Self-heal): Đảm bảo mỗi câu chỉ có DUY NHẤT 1 đáp án đúng trong DB
                        if (q != null && q.getOptions() != null) {
                            long correctCount = q.getOptions().stream().filter(QuestionOption::isCorrect).count();
                            if (correctCount > 1) {
                                QuestionOption keepOpt = q.getOptions().stream()
                                        .filter(o -> o.isCorrect() && o.getOptionKey() != null && o.getOptionKey() > 1)
                                        .findFirst()
                                        .orElse(q.getOptions().stream().filter(QuestionOption::isCorrect).findFirst().orElse(null));
                                if (keepOpt != null) {
                                    for (QuestionOption opt : q.getOptions()) {
                                        opt.setCorrect(opt.getId() != null && opt.getId().equals(keepOpt.getId()));
                                    }
                                    questionRepository.save(q);
                                }
                            }
                        }

                        // Rule: Số câu ở mỗi phần không nối tiếp nhau mà mỗi phần bắt đầu từ 1
                        int expectedNum = currentSecNum++;
                        if (sq.getQuestionNumber() == null || sq.getQuestionNumber() != expectedNum) {
                            sq.setQuestionNumber(expectedNum);
                            examSectionQuestionRepository.save(sq);
                        }
                        reviewQuestions.add(toReviewQuestionResponse(q, expectedNum));
                    }

                    return ReviewSectionResponse.builder()
                            .id(s.getId())
                            .sessionType(s.getSessionType())
                            .sectionTitle(s.getSectionTitle())
                            .audioUrl(s.getAudioUrl())
                            .orderIndex(s.getOrderIndex())
                            .durationMinutes(s.getDurationMinutes())
                            .questions(reviewQuestions)
                            .build();
                })
                .collect(Collectors.toList());

        AdminExamPreviewResponse preview = examMapper.toAdminPreviewResponse(exam);
        preview.setScoreGroups(scoreGroups);
        preview.setSections(reviewSections);

        long totalAttempts = examAttemptRepository.countByExamIdAndDeletedFalse(examId);
        long inProgressAttempts = examAttemptRepository.countByExamIdAndStatusAndDeletedFalse(examId, AttemptStatus.IN_PROGRESS);
        preview.setTotalAttempts(totalAttempts);
        preview.setInProgressAttempts(inProgressAttempts);

        return preview;
    }

    @Override
    public void updateQuestionInline(Long examId, Long questionId, UpdateQuestionInlineRequest request) {
        Question question = questionRepository.findByIdWithOptionsAndPassage(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        if (request.getQuestionText() != null) {
            question.setQuestionText(request.getQuestionText());
        }
        if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }
        if (request.getAudioUrl() != null) {
            question.setAudioUrl(request.getAudioUrl());
        }
        if (request.getAudioStartTime() != null) {
            question.setAudioStartTime(request.getAudioStartTime());
        }
        if (request.getAudioEndTime() != null) {
            question.setAudioEndTime(request.getAudioEndTime());
        }
        if (request.getTranscriptJp() != null) {
            question.setTranscriptJp(request.getTranscriptJp());
        }
        if (request.getTranscriptVi() != null) {
            question.setTranscriptVi(request.getTranscriptVi());
        }
        if (request.getStarPosition() != null) {
            question.setStarPosition(request.getStarPosition());
        }
        if (request.getExplanationVi() != null) {
            question.setExplanationVi(request.getExplanationVi());
        }
        if (request.getHasVisualOptions() != null) {
            question.setHasVisualOptions(request.getHasVisualOptions());
        }
        if (request.getMondaiCode() != null) {
            mondaiTypeRepository.findByCodeAndDeletedFalse(request.getMondaiCode())
                    .ifPresent(question::setMondaiType);
        }

        // Rule: Mỗi câu chỉ có DUY NHẤT 1 đáp án đúng
        Short determinedCorrectKey = request.getCorrectOptionKey();
        if (determinedCorrectKey == null && request.getOptions() != null) {
            for (var optReq : request.getOptions()) {
                if (Boolean.TRUE.equals(optReq.getIsCorrect())) {
                    determinedCorrectKey = optReq.getOptionKey();
                    break;
                }
            }
        }
        final Short finalCorrectKey = determinedCorrectKey;

        if (finalCorrectKey != null) {
            for (QuestionOption opt : question.getOptions()) {
                opt.setCorrect(opt.getOptionKey() != null &&
                        opt.getOptionKey().shortValue() == finalCorrectKey.shortValue());
            }
        }

        // Cập nhật chi tiết text, image của từng option (và giữ đúng 1 đáp án đúng)
        if (request.getOptions() != null) {
            for (var optReq : request.getOptions()) {
                question.getOptions().stream()
                        .filter(o -> (optReq.getId() != null && optReq.getId().equals(o.getId()))
                                || (optReq.getOptionKey() != null && o.getOptionKey() != null &&
                                    o.getOptionKey().shortValue() == optReq.getOptionKey().shortValue()))
                        .findFirst()
                        .ifPresent(o -> {
                            if (optReq.getOptionText() != null) o.setOptionText(optReq.getOptionText());
                            if (optReq.getOptionImageUrl() != null) o.setOptionImageUrl(optReq.getOptionImageUrl());
                            if (finalCorrectKey != null && o.getOptionKey() != null) {
                                o.setCorrect(o.getOptionKey().shortValue() == finalCorrectKey.shortValue());
                            } else if (optReq.getIsCorrect() != null) {
                                o.setCorrect(optReq.getIsCorrect());
                            }
                        });
            }
        }

        // Cập nhật hoặc tạo mới bài đọc (ReadingPassage) cho câu hỏi đọc hiểu (hỗ trợ cả văn bản chữ lẫn ảnh tờ rơi/thông báo)
        boolean hasPassageData = (request.getPassageContentJp() != null && !request.getPassageContentJp().isBlank())
                || (request.getPassageImageUrl() != null && !request.getPassageImageUrl().isBlank());
        if (hasPassageData) {
            ReadingPassage passage = question.getPassage();
            if (passage == null) {
                passage = ReadingPassage.builder()
                        .title(request.getPassageTitle() != null ? request.getPassageTitle() : "Bài đọc hiểu")
                        .jlptLevel(question.getJlptLevel())
                        .contentJp(request.getPassageContentJp())
                        .translationVi(request.getPassageTranslationVi())
                        .imageUrl(request.getPassageImageUrl())
                        .active(true)
                        .build();
                passage = readingPassageRepository.save(passage);
                question.setPassage(passage);
            } else {
                if (request.getPassageTitle() != null) passage.setTitle(request.getPassageTitle());
                if (request.getPassageContentJp() != null) passage.setContentJp(request.getPassageContentJp());
                if (request.getPassageTranslationVi() != null) passage.setTranslationVi(request.getPassageTranslationVi());
                if (request.getPassageImageUrl() != null) passage.setImageUrl(request.getPassageImageUrl());
                readingPassageRepository.save(passage);
            }
        }

        questionRepository.save(question);
    }

    @Override
    public Long addQuestion(Long examId, Long sectionId, CreateQuestionRequest request, Long currentUserId) {
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        ExamSection section = examSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        if (!section.getExam().getId().equals(exam.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        ReadingPassage passage = null;
        boolean hasPassageRequest = (request.getPassageContentJp() != null && !request.getPassageContentJp().isBlank())
                || (request.getPassageImageUrl() != null && !request.getPassageImageUrl().isBlank());

        if (request.getPassageId() != null) {
            passage = readingPassageRepository.findById(request.getPassageId()).orElse(null);
        } else if (hasPassageRequest) {
            String trimmedJp = request.getPassageContentJp() != null ? request.getPassageContentJp().trim() : "";
            String imgUrl = request.getPassageImageUrl() != null ? request.getPassageImageUrl().trim() : "";
            var existingSecQuestions = examSectionQuestionRepository.findBySectionIdWithQuestionDetails(section.getId());
            for (var esq : existingSecQuestions) {
                var p = esq.getQuestion().getPassage();
                if (p != null) {
                    boolean matchContent = !trimmedJp.isEmpty() && p.getContentJp() != null && p.getContentJp().trim().equals(trimmedJp);
                    boolean matchImage = !imgUrl.isEmpty() && p.getImageUrl() != null && p.getImageUrl().trim().equals(imgUrl);
                    if (matchContent || matchImage) {
                        passage = p;
                        break;
                    }
                }
            }

            if (passage == null) {
                passage = ReadingPassage.builder()
                        .title(request.getPassageTitle() != null && !request.getPassageTitle().isBlank() ? request.getPassageTitle() : "Bài đọc hiểu")
                        .jlptLevel(exam.getJlptLevel())
                        .contentJp(request.getPassageContentJp())
                        .translationVi(request.getPassageTranslationVi())
                        .imageUrl(request.getPassageImageUrl())
                        .active(true)
                        .build();
                passage = readingPassageRepository.save(passage);
            }
        }

        MondaiType mondaiType = null;
        if (request.getMondaiCode() != null) {
            mondaiType = mondaiTypeRepository.findByCodeAndDeletedFalse(request.getMondaiCode()).orElse(null);
        }
        if (mondaiType == null) {
            SkillType targetSkill = switch (section.getSessionType()) {
                case LISTENING -> SkillType.LISTENING;
                case GRAMMAR_READING -> SkillType.READING;
                case KNOWLEDGE_READING -> (passage != null ? SkillType.READING : SkillType.VOCABULARY);
                default -> SkillType.VOCABULARY;
            };
            mondaiType = mondaiTypeRepository.findBySkillTypeAndActiveTrueAndDeletedFalse(targetSkill)
                    .stream().findFirst().orElseGet(() ->
                            mondaiTypeRepository.findAll().stream().findFirst().orElse(null)
                    );
        }

        Question question = new Question();
        question.setJlptLevel(exam.getJlptLevel());
        question.setSkillType(mondaiType != null ? mondaiType.getSkillType() : SkillType.VOCABULARY);
        question.setMondaiType(mondaiType);
        question.setPassage(passage);
        question.setQuestionText(request.getQuestionText());
        question.setImageUrl(request.getImageUrl());
        question.setAudioUrl(request.getAudioUrl() != null && !request.getAudioUrl().isBlank()
                ? request.getAudioUrl()
                : section.getAudioUrl());
        question.setAudioStartTime(request.getAudioStartTime());
        question.setAudioEndTime(request.getAudioEndTime());
        question.setTranscriptJp(request.getTranscriptJp());
        question.setTranscriptVi(request.getTranscriptVi());
        question.setStarPosition(request.getStarPosition());
        question.setHasVisualOptions(Boolean.TRUE.equals(request.getHasVisualOptions()));
        question.setExplanationVi(request.getExplanationVi());
        question.setActive(true);
        question.setCreatedBy(creator);

        short optKey = 1;
        if (request.getOptions() != null) {
            // Rule: Mỗi câu trắc nghiệm JLPT chỉ có DUY NHẤT 1 đáp án đúng
            Integer targetCorrectKey = null;
            for (var optReq : request.getOptions()) {
                if (Boolean.TRUE.equals(optReq.getIsCorrect())) {
                    if (targetCorrectKey == null || (optReq.getOptionKey() != null && optReq.getOptionKey() > 1)) {
                        targetCorrectKey = optReq.getOptionKey() != null ? optReq.getOptionKey().intValue() : 1;
                    }
                }
            }
            if (targetCorrectKey == null) {
                targetCorrectKey = 1;
            }

            for (var optReq : request.getOptions()) {
                short currentKey = optReq.getOptionKey() != null ? optReq.getOptionKey() : optKey++;
                boolean isCorrect = (currentKey == targetCorrectKey.shortValue());
                QuestionOption option = QuestionOption.builder()
                        .optionKey(currentKey)
                        .optionText(optReq.getOptionText())
                        .optionImageUrl(optReq.getOptionImageUrl())
                        .correct(isCorrect)
                        .build();
                question.addOption(option);
            }
        }

        question = questionRepository.save(question);

        var existingQuestions = examSectionQuestionRepository.findBySectionIdWithQuestionDetails(section.getId());
        int nextQNum = request.getQuestionNumber() != null ? request.getQuestionNumber() : existingQuestions.size() + 1;

        ExamSectionQuestion esq = ExamSectionQuestion.builder()
                .exam(exam)
                .section(section)
                .question(question)
                .questionNumber(nextQNum)
                .points(request.getPoints() != null ? request.getPoints() : new BigDecimal("1.00"))
                .build();

        examSectionQuestionRepository.save(esq);

        exam.setTotalQuestions(exam.getTotalQuestions() != null ? exam.getTotalQuestions() + 1 : 1);
        examRepository.save(exam);

        // Tự động sắp xếp và đánh số lại toàn bộ câu hỏi liên tục theo chuẩn JLPT
        reorderExamQuestions(examId);

        log.info("✅ Giáo viên đã thêm câu hỏi ID: {} vào Section ID: {} của Exam ID: {}", question.getId(), section.getId(), exam.getId());
        return question.getId();
    }

    private void validateNoInProgressAttempts(Long examId, String actionDescription) {
        long inProgressCount = examAttemptRepository.countByExamIdAndStatusAndDeletedFalse(examId, AttemptStatus.IN_PROGRESS);
        if (inProgressCount > 0) {
            log.warn("⚠️ Từ chối {}: Đang có {} thí sinh làm bài trực tiếp cho Exam ID: {}", actionDescription, inProgressCount, examId);
            throw new AppException(ErrorCode.EXAM_HAS_IN_PROGRESS_ATTEMPTS);
        }
    }

    @Override
    public void deleteQuestion(Long examId, Long questionId) {
        validateNoInProgressAttempts(examId, "xóa câu hỏi (Question ID: " + questionId + ")");

        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        var esqList = examSectionQuestionRepository.findByExamIdAndQuestionId(examId, questionId);
        for (var esq : esqList) {
            esq.softDelete();
            examSectionQuestionRepository.save(esq);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));
        question.softDelete();
        questionRepository.save(question);

        exam.setTotalQuestions(Math.max(0, (exam.getTotalQuestions() != null ? exam.getTotalQuestions() : 1) - 1));
        examRepository.save(exam);

        // Tự động sắp xếp và đánh số lại câu hỏi để khép kín khoảng trống
        reorderExamQuestions(examId);

        log.info("🗑️ Đã xóa câu hỏi ID: {} khỏi Exam ID: {}", questionId, examId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void clearSectionQuestions(Long examId, Long sectionId) {
        validateNoInProgressAttempts(examId, "xóa toàn bộ câu hỏi Section (Section ID: " + sectionId + ")");

        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        List<ExamSectionQuestion> esqList = examSectionQuestionRepository.findBySectionIdAndDeletedFalseOrderByQuestionNumberAsc(sectionId);
        if (esqList.isEmpty()) {
            return;
        }

        int removedCount = esqList.size();
        for (ExamSectionQuestion esq : esqList) {
            esq.softDelete();
            examSectionQuestionRepository.save(esq);

            Question q = esq.getQuestion();
            if (q != null) {
                q.softDelete();
                questionRepository.save(q);
            }
        }

        exam.setTotalQuestions(Math.max(0, (exam.getTotalQuestions() != null ? exam.getTotalQuestions() : 0) - removedCount));
        examRepository.save(exam);

        log.info("🗑️ Đã xóa toàn bộ {} câu hỏi trong Section ID: {} của Exam ID: {}", removedCount, sectionId, examId);
    }

    @Override
    public void updatePassage(Long examId, Long passageId, com.jp.elearningjp.dto.request.exam.UpdatePassageRequest request) {
        ReadingPassage passage = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        if (request.getTitle() != null) passage.setTitle(request.getTitle());
        passage.setContentJp(request.getContentJp());
        passage.setImageUrl(request.getImageUrl());
        if (request.getTranslationVi() != null) passage.setTranslationVi(request.getTranslationVi());

        readingPassageRepository.save(passage);
        log.info("✅ Đã cập nhật bài đọc ID: {} cho Exam ID: {}", passageId, examId);
    }

    @Override
    public void deletePassage(Long examId, Long passageId) {
        validateNoInProgressAttempts(examId, "xóa bài đọc (Passage ID: " + passageId + ")");

        ReadingPassage passage = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_QUESTION_NOT_FOUND));

        var examSectionQuestions = examSectionQuestionRepository.findByExamIdWithQuestionDetails(examId);
        int deletedCount = 0;
        for (var esq : examSectionQuestions) {
            Question q = esq.getQuestion();
            if (q != null && q.getPassage() != null && q.getPassage().getId().equals(passageId)) {
                esq.softDelete();
                examSectionQuestionRepository.save(esq);
                q.softDelete();
                questionRepository.save(q);
                deletedCount++;
            }
        }

        passage.softDelete();
        readingPassageRepository.save(passage);

        Exam exam = examRepository.findByIdAndDeletedFalse(examId).orElse(null);
        if (exam != null && deletedCount > 0) {
            exam.setTotalQuestions(Math.max(0, (exam.getTotalQuestions() != null ? exam.getTotalQuestions() : deletedCount) - deletedCount));
            examRepository.save(exam);
            reorderExamQuestions(examId);
        }

        log.info("🗑️ Đã xóa bài đọc ID: {} và {} câu hỏi liên kết khỏi Exam ID: {}", passageId, deletedCount, examId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ExamSummaryResponse> getCourseExams(Long courseId) {
        return examRepository.findByCourseIdAndDeletedFalse(courseId).stream()
                .map(examMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void publishExam(Long examId) {
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        exam.setPublished(true);
        examRepository.save(exam);
    }

    @Override
    public void unpublishExam(Long examId) {
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        exam.setPublished(false);
        examRepository.save(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ExamSummaryResponse> getAllExams(
            JlptLevel level, ExamType examType, Boolean published, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Exam> exams;
        if (published != null) {
            if (level != null) {
                exams = examRepository.findByJlptLevelAndPublishedAndDeletedFalse(level, published, pageable);
            } else {
                exams = examRepository.findByPublishedAndDeletedFalse(published, pageable);
            }
        } else {
            if (level != null) {
                exams = examRepository.findByJlptLevelAndDeletedFalse(level, pageable);
            } else {
                exams = examRepository.findByDeletedFalse(pageable);
            }
        }
        return exams.map(examMapper::toSummaryResponse);
    }

    @Override
    public void deleteExam(Long examId) {
        validateNoInProgressAttempts(examId, "xóa đề thi");

        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        exam.softDelete();
        examRepository.save(exam);
    }

    @Override
    public void updateSectionAudio(Long examId, Long sectionId, String audioUrl) {
        ExamSection section = examSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
        section.setAudioUrl(audioUrl);
        examSectionRepository.save(section);

        var sectionQuestions = examSectionQuestionRepository.findBySectionIdWithQuestionDetails(sectionId);
        for (var sq : sectionQuestions) {
            Question q = sq.getQuestion();
            if (q != null) {
                q.setAudioUrl(audioUrl);
                questionRepository.save(q);
            }
        }
    }

    private final java.util.Map<Long, AdminExamPreviewResponse> memorySnapshots = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void saveSnapshot(Long examId, AdminExamPreviewResponse snapshot) {
        if (snapshot == null) {
            snapshot = getExamPreview(examId);
        }
        memorySnapshots.put(examId, snapshot);
        log.info("📸 Đã lưu snapshot cho Exam ID: {} ({} câu hỏi)", examId, snapshot.getTotalQuestions());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminExamPreviewResponse getSnapshot(Long examId) {
        AdminExamPreviewResponse snapshot = memorySnapshots.get(examId);
        if (snapshot == null) {
            snapshot = getExamPreview(examId);
            memorySnapshots.put(examId, snapshot);
        }
        return snapshot;
    }

    @Override
    @Transactional
    public AdminExamPreviewResponse rollbackExam(Long examId, AdminExamPreviewResponse snapshot) {
        if (snapshot == null) {
            snapshot = memorySnapshots.get(examId);
        }
        if (snapshot == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        // 1. Soft-delete toàn bộ các câu hỏi hiện tại trong đề thi
        List<ExamSectionQuestion> currentEsqs = examSectionQuestionRepository
                .findByExamIdAndDeletedFalseOrderByQuestionNumberAsc(examId);
        for (ExamSectionQuestion esq : currentEsqs) {
            esq.softDelete();
            examSectionQuestionRepository.save(esq);
            if (esq.getQuestion() != null) {
                esq.getQuestion().softDelete();
                questionRepository.save(esq.getQuestion());
            }
        }

        // 2. Khôi phục lại từng section và câu hỏi theo snapshot
        if (snapshot.getSections() != null) {
            for (ReviewSectionResponse secSnap : snapshot.getSections()) {
                ExamSection section = exam.getSections().stream()
                        .filter(s -> !s.isDeleted() && (s.getId().equals(secSnap.getId()) || s.getSessionType() == secSnap.getSessionType()))
                        .findFirst()
                        .orElse(null);

                if (section == null) {
                    continue;
                }

                if (secSnap.getSectionTitle() != null) section.setSectionTitle(secSnap.getSectionTitle());
                section.setAudioUrl(secSnap.getAudioUrl());
                if (secSnap.getDurationMinutes() != null) section.setDurationMinutes(secSnap.getDurationMinutes());
                examSectionRepository.save(section);

                java.util.Map<String, ReadingPassage> restoredPassages = new java.util.HashMap<>();
                if (secSnap.getQuestions() != null) {
                    for (ReviewQuestionResponse qSnap : secSnap.getQuestions()) {
                        ReadingPassage passage = null;
                        if (qSnap.getPassage() != null && qSnap.getPassage().getContentJp() != null && !qSnap.getPassage().getContentJp().isBlank()) {
                            String pKey = qSnap.getPassage().getContentJp().trim();
                            if (restoredPassages.containsKey(pKey)) {
                                passage = restoredPassages.get(pKey);
                            } else {
                                passage = ReadingPassage.builder()
                                        .title(qSnap.getPassage().getTitle() != null ? qSnap.getPassage().getTitle() : "Bài đọc hiểu")
                                        .jlptLevel(exam.getJlptLevel())
                                        .contentJp(qSnap.getPassage().getContentJp())
                                        .passageType(qSnap.getPassage().getPassageType() != null ? qSnap.getPassage().getPassageType() : PassageType.TEXT)
                                        .imageUrl(qSnap.getPassage().getImageUrl())
                                        .active(true)
                                        .build();
                                passage = readingPassageRepository.save(passage);
                                restoredPassages.put(pKey, passage);
                            }
                        }

                        MondaiType mondaiType = null;
                        if (qSnap.getMondaiCode() != null) {
                            mondaiType = mondaiTypeRepository.findByCodeAndDeletedFalse(qSnap.getMondaiCode()).orElse(null);
                        }
                        if (mondaiType == null) {
                            SkillType targetSkill = switch (section.getSessionType()) {
                                case LISTENING -> SkillType.LISTENING;
                                case GRAMMAR_READING -> SkillType.READING;
                                case KNOWLEDGE_READING -> (qSnap.getPassage() != null ? SkillType.READING : SkillType.VOCABULARY);
                                default -> SkillType.VOCABULARY;
                            };
                            mondaiType = mondaiTypeRepository.findBySkillTypeAndActiveTrueAndDeletedFalse(targetSkill)
                                    .stream().findFirst().orElse(null);
                        }

                        Question question = new Question();
                        question.setJlptLevel(exam.getJlptLevel());
                        question.setSkillType(mondaiType != null ? mondaiType.getSkillType() : SkillType.VOCABULARY);
                        question.setMondaiType(mondaiType);
                        question.setPassage(passage);
                        question.setQuestionText(qSnap.getQuestionText() != null ? qSnap.getQuestionText() : "");
                        question.setExplanationVi(qSnap.getExplanationVi());
                        question.setImageUrl(qSnap.getImageUrl());
                        question.setAudioUrl(qSnap.getAudioUrl());
                        question.setAudioStartTime(qSnap.getAudioStartTime());
                        question.setAudioEndTime(qSnap.getAudioEndTime());
                        question.setTranscriptJp(qSnap.getTranscriptJp());
                        question.setTranscriptVi(qSnap.getTranscriptVi());
                        question.setStarPosition(qSnap.getStarPosition());
                        question.setHasVisualOptions(qSnap.isHasVisualOptions());
                        question.setActive(true);

                        if (qSnap.getOptions() != null) {
                            for (ReviewOptionResponse optSnap : qSnap.getOptions()) {
                                QuestionOption option = QuestionOption.builder()
                                        .optionKey(optSnap.getOptionKey())
                                        .optionText(optSnap.getOptionText() != null ? optSnap.getOptionText() : "")
                                        .optionImageUrl(optSnap.getOptionImageUrl())
                                        .correct(optSnap.isCorrect())
                                        .build();
                                question.addOption(option);
                            }
                        }
                        question = questionRepository.save(question);

                        ExamSectionQuestion esq = ExamSectionQuestion.builder()
                                .exam(exam)
                                .section(section)
                                .question(question)
                                .questionNumber(qSnap.getQuestionNumber())
                                .build();
                        examSectionQuestionRepository.save(esq);
                    }
                }
            }
        }

        exam.setTotalQuestions(snapshot.getTotalQuestions() != null ? snapshot.getTotalQuestions() : 0);
        examRepository.save(exam);

        log.info("⏪ Đã rollback hoàn toàn đề thi ID: {} về trạng thái snapshot ({} câu hỏi)", examId, exam.getTotalQuestions());
        return getExamPreview(examId);
    }

    private ReviewQuestionResponse toReviewQuestionResponse(Question q, Integer questionNumber) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toReviewOptionResponse)
                .collect(Collectors.toList());

        // Defensive Rule: Đảm bảo options trả về chỉ có tối đa 1 đáp án đúng
        long correctCount = options.stream().filter(ReviewOptionResponse::isCorrect).count();
        if (correctCount > 1) {
            ReviewOptionResponse keep = options.stream()
                    .filter(o -> o.isCorrect() && o.getOptionKey() != null && o.getOptionKey() > 1)
                    .findFirst()
                    .orElse(options.stream().filter(ReviewOptionResponse::isCorrect).findFirst().orElse(null));
            if (keep != null) {
                for (var opt : options) {
                    opt.setCorrect(opt.equals(keep) || (opt.getOptionKey() != null && opt.getOptionKey().equals(keep.getOptionKey())));
                }
            }
        }

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        ReviewQuestionResponse resp = examMapper.toReviewQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);

        // Rule: Những câu nghe chỉ có chữ (KEY_POINT, QUICK_RESPONSE hoặc các câu TASK_BASED không tranh)
        String mCode = q.getMondaiType() != null ? q.getMondaiType().getCode() : "";
        boolean isKeyPointOrQuick = "KEY_POINT".equals(mCode) || "QUICK_RESPONSE".equals(mCode);
        boolean hasAnyOptionImage = options != null && options.stream().anyMatch(o -> o.getOptionImageUrl() != null && !o.getOptionImageUrl().isBlank());
        boolean isTextOnlyTaskBased = false;
        // Chỉ coi là câu thuần chữ khi câu hỏi KHÔNG có ảnh đề bài trong DB, KHÔNG có hasVisualOptions và KHÔNG có ảnh lựa chọn
        if (!q.isHasVisualOptions() && !hasAnyOptionImage && !"UTTERANCE_EXPRESSIONS".equals(mCode) && (q.getImageUrl() == null || q.getImageUrl().isBlank()) && options != null) {
            long meaningfulCount = options.stream()
                    .filter(o -> {
                        String t = o.getOptionText() != null ? o.getOptionText().trim() : "";
                        if (t.length() <= 1) return false;
                        if (t.startsWith("Lựa chọn") || t.startsWith("Đáp án") ||
                            t.startsWith("Option") || t.startsWith("Phương án") ||
                            t.startsWith("選択肢")) return false;
                        // Nhãn tranh (như "アイ", "アエ", "A", "1"...) không phải là chữ thực tế
                        if (t.matches("^[ア-ンA-Za-z0-9\\s・、,/\\-()（）①-④]+$")) return false;
                        return true;
                    })
                    .count();
            if (meaningfulCount >= 2) {
                isTextOnlyTaskBased = true;
            }
        }
        if (isKeyPointOrQuick || isTextOnlyTaskBased) {
            resp.setImageUrl(null);
            resp.setHasVisualOptions(false);
            if (options != null) {
                for (var opt : options) {
                    opt.setOptionImageUrl(null);
                }
            }
        }
        return resp;
    }

    @Override
    @Transactional
    public AdminExamPreviewResponse reorderExamQuestions(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        var sections = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparingInt(ExamSection::getOrderIndex))
                .collect(Collectors.toList());

        int currentQuestionNumber = 1;

        // Thứ tự chuẩn của các Mondai Code trong JLPT
        List<String> mondaiPriorityOrder = List.of(
                // Từ vựng (Vocabulary)
                "KANJI_READING", "ORTHOGRAPHY", "WORD_FORMATION", "CONTEXTUAL_USE", "SYNONYMS", "PROPER_USAGE",
                // Ngữ pháp & Đọc hiểu (Grammar & Reading)
                "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION", "TEXT_GRAMMAR",
                "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE", "INTEGRATED_COMPREHENSION", "THEMATIC_COMPREHENSION", "INFORMATION_RETRIEVAL",
                // Nghe hiểu (Listening)
                "TASK_BASED", "KEY_POINT", "GENERAL_UNDERSTANDING", "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE", "INTEGRATED_LISTENING"
        );

        int totalQuestions = 0;
        for (ExamSection section : sections) {
            List<ExamSectionQuestion> esqList = examSectionQuestionRepository.findBySectionIdWithQuestionDetails(section.getId());
            if (esqList.isEmpty()) {
                continue;
            }

            // Sắp xếp các câu hỏi trong Section
            esqList.sort((a, b) -> {
                String codeA = a.getQuestion() != null && a.getQuestion().getMondaiType() != null
                        ? a.getQuestion().getMondaiType().getCode() : "";
                String codeB = b.getQuestion() != null && b.getQuestion().getMondaiType() != null
                        ? b.getQuestion().getMondaiType().getCode() : "";

                int idxA = mondaiPriorityOrder.indexOf(codeA);
                int idxB = mondaiPriorityOrder.indexOf(codeB);
                if (idxA == -1) idxA = 999;
                if (idxB == -1) idxB = 999;

                if (idxA != idxB) {
                    return Integer.compare(idxA, idxB);
                }

                // Cùng Mondai: các câu hỏi có cùng bài đọc (passage_id) đi liền kề nhau
                Long pIdA = a.getQuestion() != null && a.getQuestion().getPassage() != null
                        ? a.getQuestion().getPassage().getId() : null;
                Long pIdB = b.getQuestion() != null && b.getQuestion().getPassage() != null
                        ? b.getQuestion().getPassage().getId() : null;

                if (pIdA != null && pIdB != null && !pIdA.equals(pIdB)) {
                    return Long.compare(pIdA, pIdB);
                } else if (pIdA != null && pIdB == null) {
                    return -1;
                } else if (pIdA == null && pIdB != null) {
                    return 1;
                }

                // So sánh theo questionNumber hiện tại hoặc ID
                int numA = a.getQuestionNumber() != null ? a.getQuestionNumber() : 0;
                int numB = b.getQuestionNumber() != null ? b.getQuestionNumber() : 0;
                if (numA != numB) {
                    return Integer.compare(numA, numB);
                }
                return Long.compare(a.getId(), b.getId());
            });

            // Rule: Số câu ở mỗi phần không nối tiếp nhau mà mỗi phần bắt đầu từ 1
            int secQuestionNumber = 1;
            for (ExamSectionQuestion esq : esqList) {
                esq.setQuestionNumber(secQuestionNumber++);
                examSectionQuestionRepository.save(esq);
                totalQuestions++;
            }
        }

        exam.setTotalQuestions(totalQuestions);
        examRepository.save(exam);

        log.info("🔢 Đã đánh số lại toàn bộ {} câu hỏi của đề thi ID: {} (mỗi phần bắt đầu từ 1)", totalQuestions, examId);
        return getExamPreview(examId);
    }

    @Override
    public Long cloneExam(Long examId, Long currentUserId) {
        Exam source = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(source.getCreatedBy()) : source.getCreatedBy();

        Exam target = Exam.builder()
                .title(source.getTitle() + " (Bản sao mới)")
                .jlptLevel(source.getJlptLevel())
                .examType(source.getExamType())
                .totalDurationMinutes(source.getTotalDurationMinutes())
                .totalScaledMaxScore(source.getTotalScaledMaxScore())
                .overallPassingScore(source.getOverallPassingScore())
                .sourceFileName(source.getSourceFileName())
                .published(false)
                .createdBy(creator)
                .course(source.getCourse())
                .build();

        if (source.getScoreGroups() != null) {
            for (ExamScoreGroup sg : source.getScoreGroups()) {
                ExamScoreGroup newSg = ExamScoreGroup.builder()
                        .groupCode(sg.getGroupCode())
                        .maxScaledScore(sg.getMaxScaledScore())
                        .minRequiredScore(sg.getMinRequiredScore())
                        .build();
                target.addScoreGroup(newSg);
            }
        }

        target = examRepository.save(target);

        int totalClonedQuestions = 0;
        if (source.getSections() != null) {
            for (ExamSection s : source.getSections()) {
                if (s.isDeleted()) continue;
                ExamSection newSection = ExamSection.builder()
                        .exam(target)
                        .sessionType(s.getSessionType())
                        .sectionTitle(s.getSectionTitle())
                        .audioUrl(s.getAudioUrl())
                        .orderIndex(s.getOrderIndex())
                        .durationMinutes(s.getDurationMinutes())
                        .build();
                newSection = examSectionRepository.save(newSection);

                var esqList = examSectionQuestionRepository.findBySectionIdWithQuestionDetails(s.getId());
                for (var esq : esqList) {
                    if (esq.isDeleted()) continue;
                    Question q = esq.getQuestion();
                    if (q.isDeleted()) continue;

                    Question newQ = new Question();
                    newQ.setJlptLevel(q.getJlptLevel());
                    newQ.setSkillType(q.getSkillType());
                    newQ.setMondaiType(q.getMondaiType());
                    newQ.setPassage(q.getPassage());
                    newQ.setQuestionText(q.getQuestionText());
                    newQ.setImageUrl(q.getImageUrl());
                    newQ.setAudioUrl(q.getAudioUrl());
                    newQ.setAudioStartTime(q.getAudioStartTime());
                    newQ.setAudioEndTime(q.getAudioEndTime());
                    newQ.setTranscriptJp(q.getTranscriptJp());
                    newQ.setTranscriptVi(q.getTranscriptVi());
                    newQ.setExplanationVi(q.getExplanationVi());
                    newQ.setStarPosition(q.getStarPosition());

                    if (q.getOptions() != null) {
                        for (QuestionOption opt : q.getOptions()) {
                            if (opt.isDeleted()) continue;
                            QuestionOption newOpt = QuestionOption.builder()
                                    .optionKey(opt.getOptionKey())
                                    .optionText(opt.getOptionText())
                                    .optionImageUrl(opt.getOptionImageUrl())
                                    .correct(opt.isCorrect())
                                    .build();
                            newQ.addOption(newOpt);
                        }
                    }
                    newQ = questionRepository.save(newQ);

                    ExamSectionQuestion newEsq = ExamSectionQuestion.builder()
                            .exam(target)
                            .section(newSection)
                            .question(newQ)
                            .questionNumber(esq.getQuestionNumber())
                            .points(esq.getPoints())
                            .build();
                    examSectionQuestionRepository.save(newEsq);
                    totalClonedQuestions++;
                }
            }
        }

        target.setTotalQuestions(totalClonedQuestions);
        examRepository.save(target);

        log.info("📋 Đã nhân bản đề thi ID: {} sang đề mới ID: {} với {} câu hỏi", source.getId(), target.getId(), totalClonedQuestions);
        return target.getId();
    }

    @Override
    public int regradeExam(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        var attempts = examAttemptRepository.findByExamIdAndDeletedFalse(examId).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.TIMED_OUT)
                .collect(Collectors.toList());

        if (attempts.isEmpty()) {
            log.info("ℹ️ Không có bài thi nào đã nộp để chấm lại cho đề thi ID: {}", examId);
            return 0;
        }

        var allSectionQuestions = examSectionQuestionRepository.findByExamIdWithQuestionDetails(examId);

        for (ExamAttempt attempt : attempts) {
            var studentAnswers = studentAnswerRepository.findByAttemptIdWithDetails(attempt.getId());
            var answerMap = studentAnswers.stream()
                    .collect(Collectors.toMap(a -> a.getQuestion().getId(), java.util.function.Function.identity(), (e1, e2) -> e1));

            BigDecimal totalRawScore = BigDecimal.ZERO;
            for (ExamSectionQuestion sq : allSectionQuestions) {
                StudentAnswer ans = answerMap.get(sq.getQuestion().getId());
                if (ans != null && ans.getSelectedOption() != null) {
                    boolean isCorrect = ans.getSelectedOption().isCorrect();
                    ans.setCorrect(isCorrect);
                    BigDecimal points = isCorrect ? sq.getPoints() : BigDecimal.ZERO;
                    ans.setPointsEarned(points);
                    totalRawScore = totalRawScore.add(points);
                }
            }
            studentAnswerRepository.saveAll(studentAnswers);
        }

        log.info("🎯 Đã chấm lại (re-grade) thành công cho {} lượt thi của đề thi ID: {}", attempts.size(), examId);
        return attempts.size();
    }
}

