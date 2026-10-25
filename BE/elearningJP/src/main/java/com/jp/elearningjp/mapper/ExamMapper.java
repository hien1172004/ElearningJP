package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.response.exam.*;
import com.jp.elearningjp.entity.content.ReadingPassage;
import com.jp.elearningjp.entity.exam.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExamMapper {

    ExamSummaryResponse toSummaryResponse(Exam exam);

    @Mapping(target = "scoreGroups", ignore = true)
    @Mapping(target = "sections", ignore = true)
    ExamDetailResponse toDetailResponse(Exam exam);

    @Mapping(target = "scoreGroups", ignore = true)
    @Mapping(target = "sections", ignore = true)
    AdminExamPreviewResponse toAdminPreviewResponse(Exam exam);

    ExamScoreGroupResponse toScoreGroupResponse(ExamScoreGroup scoreGroup);

    @Mapping(target = "questionCount", ignore = true)
    ExamSectionSummaryResponse toSectionSummaryResponse(ExamSection section);

    TakingPassageResponse toTakingPassageResponse(ReadingPassage passage);

    TakingOptionResponse toTakingOptionResponse(QuestionOption option);

    ReviewOptionResponse toReviewOptionResponse(QuestionOption option);

    @Mapping(target = "options", ignore = true)
    @Mapping(target = "passage", ignore = true)
    @Mapping(target = "questionNumber", ignore = true)
    @Mapping(target = "mondaiCode", source = "mondaiType.code")
    @Mapping(target = "mondaiNameVi", source = "mondaiType.nameVi")
    @Mapping(target = "instructionJp", source = "mondaiType.instructionJp")
    @Mapping(target = "instructionVi", source = "mondaiType.instructionVi")
    TakingQuestionResponse toTakingQuestionResponse(Question question);

    @Mapping(target = "options", ignore = true)
    @Mapping(target = "passage", ignore = true)
    @Mapping(target = "studentAnswer", ignore = true)
    @Mapping(target = "questionNumber", ignore = true)
    @Mapping(target = "mondaiCode", source = "mondaiType.code")
    @Mapping(target = "mondaiNameVi", source = "mondaiType.nameVi")
    @Mapping(target = "instructionJp", source = "mondaiType.instructionJp")
    @Mapping(target = "instructionVi", source = "mondaiType.instructionVi")
    ReviewQuestionResponse toReviewQuestionResponse(Question question);

    @Mapping(target = "attemptId", source = "id")
    @Mapping(target = "examId", source = "exam.id")
    @Mapping(target = "examTitle", source = "exam.title")
    @Mapping(target = "jlptLevel", source = "exam.jlptLevel")
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "sections", ignore = true)
    ExamTakingResponse toTakingResponse(ExamAttempt attempt);

    @Mapping(target = "attemptId", source = "id")
    @Mapping(target = "examId", source = "exam.id")
    @Mapping(target = "examTitle", source = "exam.title")
    @Mapping(target = "jlptLevel", source = "exam.jlptLevel")
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "groupScores", ignore = true)
    @Mapping(target = "totalScaledMaxScore", source = "exam.totalScaledMaxScore")
    @Mapping(target = "overallPassingScore", source = "exam.overallPassingScore")
    ExamResultResponse toResultResponse(ExamAttempt attempt);

    @Mapping(target = "attemptId", source = "id")
    @Mapping(target = "examId", source = "exam.id")
    @Mapping(target = "examTitle", source = "exam.title")
    @Mapping(target = "jlptLevel", source = "exam.jlptLevel")
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "groupScores", ignore = true)
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "totalScaledMaxScore", source = "exam.totalScaledMaxScore")
    @Mapping(target = "overallPassingScore", source = "exam.overallPassingScore")
    ExamReviewResponse toReviewResponse(ExamAttempt attempt);

    @Mapping(target = "scoreGroupId", source = "scoreGroup.id")
    @Mapping(target = "groupCode", source = "scoreGroup.groupCode")
    @Mapping(target = "maxScaledScore", source = "scoreGroup.maxScaledScore")
    @Mapping(target = "minRequiredScore", source = "scoreGroup.minRequiredScore")
    AttemptGroupScoreResponse toAttemptGroupScoreResponse(AttemptGroupScore groupScore);

    @Mapping(target = "attemptId", source = "id")
    @Mapping(target = "examId", source = "exam.id")
    @Mapping(target = "examTitle", source = "exam.title")
    @Mapping(target = "jlptLevel", source = "exam.jlptLevel")
    @Mapping(target = "sectionTitle", source = "section.sectionTitle")
    @Mapping(target = "groupScores", ignore = true)
    @Mapping(target = "totalScaledMaxScore", source = "exam.totalScaledMaxScore")
    @Mapping(target = "overallPassingScore", source = "exam.overallPassingScore")
    ExamAttemptHistoryResponse toAttemptHistoryResponse(ExamAttempt attempt);
}
