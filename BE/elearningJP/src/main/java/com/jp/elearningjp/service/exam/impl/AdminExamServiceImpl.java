package com.jp.elearningjp.service.exam.impl;

import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.*;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamSection;
import com.jp.elearningjp.entity.exam.Question;
import com.jp.elearningjp.entity.exam.QuestionOption;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import com.jp.elearningjp.repository.exam.QuestionRepository;
import com.jp.elearningjp.service.exam.AdminExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminExamServiceImpl implements AdminExamService {

    private final ExamRepository examRepository;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamMapper examMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminExamPreviewResponse getExamPreview(Long examId) {
        Exam exam = examRepository.findByIdWithSectionsAndScoreGroups(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        var scoreGroups = exam.getScoreGroups().stream()
                .map(examMapper::toScoreGroupResponse)
                .collect(Collectors.toList());

        var reviewSections = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparingInt(ExamSection::getOrderIndex))
                .map(s -> {
                    var sectionQuestions = examSectionQuestionRepository
                            .findBySectionIdWithQuestionDetails(s.getId());

                    var reviewQuestions = sectionQuestions.stream()
                            .map(sq -> toReviewQuestionResponse(sq.getQuestion(), sq.getQuestionNumber()))
                            .collect(Collectors.toList());

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

        // Cập nhật đáp án đúng qua correctOptionKey
        if (request.getCorrectOptionKey() != null) {
            for (QuestionOption opt : question.getOptions()) {
                opt.setCorrect(opt.getOptionKey().equals(request.getCorrectOptionKey()));
            }
        }

        // Cập nhật chi tiết từng option nếu có danh sách options gửi lên
        if (request.getOptions() != null) {
            for (var optReq : request.getOptions()) {
                question.getOptions().stream()
                        .filter(o -> o.getId().equals(optReq.getId()) || o.getOptionKey().equals(optReq.getOptionKey()))
                        .findFirst()
                        .ifPresent(o -> {
                            if (optReq.getOptionText() != null) o.setOptionText(optReq.getOptionText());
                            if (optReq.getOptionImageUrl() != null) o.setOptionImageUrl(optReq.getOptionImageUrl());
                            if (optReq.getIsCorrect() != null) o.setCorrect(optReq.getIsCorrect());
                        });
            }
        }

        questionRepository.save(question);
    }

    @Override
    public void publishExam(Long examId) {
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        exam.setPublished(true);
        examRepository.save(exam);
    }

    @Override
    public void deleteExam(Long examId) {
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));

        exam.softDelete();
        examRepository.save(exam);
    }

    private ReviewQuestionResponse toReviewQuestionResponse(Question q, Integer questionNumber) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toReviewOptionResponse)
                .collect(Collectors.toList());

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        ReviewQuestionResponse resp = examMapper.toReviewQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);
        return resp;
    }
}
