package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.entity.curriculum.CoursePublishedLesson;
import com.jp.elearningjp.entity.curriculum.CoursePublishedVersion;
import com.jp.elearningjp.repository.curriculum.CoursePublishedLessonRepository;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Đọc và giải mã JSON snapshot của khóa học / bài học (qua {@link SnapshotJsonCache}). */
@Component
@RequiredArgsConstructor
public class CourseSnapshotReader {

    private final CoursePublishedVersionRepository versionRepository;
    private final CoursePublishedLessonRepository lessonRepository;
    private final SnapshotJsonCache cache;

    public Optional<CourseSnapshot> findCourse(Long publishedVersionId) {
        if (publishedVersionId == null) {
            return Optional.empty();
        }
        return cache.get("course:" + publishedVersionId, CourseSnapshot.class,
                () -> versionRepository.findById(publishedVersionId).map(CoursePublishedVersion::getContent));
    }

    public Optional<LessonSnapshot> findLesson(Long publishedVersionId, Long lessonId) {
        if (publishedVersionId == null || lessonId == null) {
            return Optional.empty();
        }
        return cache.get("lesson:" + publishedVersionId + ":" + lessonId, LessonSnapshot.class,
                () -> lessonRepository.findByPublishedVersionIdAndLessonId(publishedVersionId, lessonId)
                        .map(CoursePublishedLesson::getContent));
    }
}
