package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.service.curriculum.CourseService;
import com.jp.elearningjp.service.curriculum.EnrollmentService;
import com.jp.elearningjp.service.curriculum.LessonService;
import com.jp.elearningjp.service.curriculum.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Pure unit test cho routing — dùng MockMvc standalone (không load Spring context).
 *
 * <p>QUAN TRỌNG: Mục đích chính là verify URL pattern + HTTP method được map đúng.
 * Status code thực tế (200/201/400/500) phụ thuộc vào:
 * <ul>
 *   <li>Service bị mock → throw hoặc trả null → 500 (GET) / 201 (POST)</li>
 *   <li>Validation @Valid fail (body rỗng) → 400 (POST/PUT)</li>
 *   <li>Route sai → 404</li>
 * </ul>
 * Vì vậy assert: chỉ cần status KHÔNG PHẢI 404.
 */
class CurriculumRoutingTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // MockMvc standalone KHÔNG gọi service thực, nhưng vẫn cần inject các bean
        // (vì controller dùng @RequiredArgsConstructor + final field).
        // Tất cả method để default → Mockito trả null cho object, 0 cho primitive,
        // false cho boolean → controller có thể bị NullPointerException → status 500.
        // Đó là đủ cho mục đích verify route có map hay không.
        CourseService courseService = mock(CourseService.class);
        LessonService lessonService = mock(LessonService.class);
        EnrollmentService enrollmentService = mock(EnrollmentService.class);
        ProgressService progressService = mock(ProgressService.class);

        // Build MockMvc với Pageable resolver (cần cho endpoint có @PageableDefault)
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CourseController(courseService),
                        new LessonController(lessonService),
                        new EnrollmentController(enrollmentService),
                        new ProgressController(progressService)
                )
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ============================================================
    // Helper
    // ============================================================

    /**
     * Assert rằng route tồn tại.
     * Trong MockMvc standalone, exception từ service sẽ wrap thành NestedServletException
     * và propagate. → Catch và verify status trước exception.
     */
    private int performAndGetStatus(org.springframework.test.web.servlet.RequestBuilder request) {
        try {
            return mockMvc.perform(request)
                    .andReturn().getResponse().getStatus();
        } catch (org.springframework.web.util.NestedServletException e) {
            // Servlet wrap exception. Spring's default behavior is to send 500.
            // Trả về 500 (status mặc định khi exception xảy ra) → route mapped
            return 500;
        } catch (Exception e) {
            return 500; // route mapped
        }
    }

    private void assertRouteExists(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        int status = performAndGetStatus(request);
        if (status == 404) throw new AssertionError("Route not found");
    }

    // ============================================================
    // COURSE routes (theo CourseController thực tế)
    // ============================================================

    @Test
    void getPublicCourses_route() throws Exception {
        assertRouteExists(get("/api/v1/courses"));
    }

    @Test
    void getPublicCourses_filterByJlpt_route() throws Exception {
        assertRouteExists(get("/api/v1/courses").param("jlptLevel", "N5"));
    }

    @Test
    void getCourseDetail_route() throws Exception {
        assertRouteExists(get("/api/v1/courses/1"));
    }

    @Test
    void getCourseProgress_route() throws Exception {
        assertRouteExists(get("/api/v1/courses/1/progress"));
    }

    @Test
    void enrollCourse_route() throws Exception {
        assertRouteExists(post("/api/v1/courses/1/enroll"));
    }

    @Test
    void getAllCoursesForAdmin_route() throws Exception {
        assertRouteExists(get("/api/v1/courses/admin"));
    }

    @Test
    void createCourse_route() throws Exception {
        assertRouteExists(post("/api/v1/courses")
                .contentType("application/json")
                .content("{}"));
    }

    @Test
    void updateCourse_route() throws Exception {
        assertRouteExists(put("/api/v1/courses/1")
                .contentType("application/json")
                .content("{}"));
    }

    @Test
    void deleteCourse_route() throws Exception {
        assertRouteExists(delete("/api/v1/courses/1"));
    }

    @Test
    void publishCourse_route() throws Exception {
        assertRouteExists(patch("/api/v1/courses/1/publish").param("publish", "true"));
    }

    @Test
    void unpublishCourse_route() throws Exception {
        assertRouteExists(patch("/api/v1/courses/1/publish").param("publish", "false"));
    }

    // ============================================================
    // LESSON routes (theo LessonController thực tế)
    // ============================================================

    @Test
    void getLessonDetail_route() throws Exception {
        assertRouteExists(get("/api/v1/lessons/1"));
    }

    @Test
    void createLesson_route() throws Exception {
        assertRouteExists(post("/api/v1/courses/1/lessons")
                .contentType("application/json")
                .content("{}"));
    }

    @Test
    void updateLesson_route() throws Exception {
        assertRouteExists(put("/api/v1/lessons/1")
                .contentType("application/json")
                .content("{}"));
    }

    @Test
    void deleteLesson_route() throws Exception {
        assertRouteExists(delete("/api/v1/lessons/1"));
    }

    @Test
    void completeLessonByLessonRoute_route() throws Exception {
        assertRouteExists(post("/api/v1/lessons/1/complete"));
    }

    // ============================================================
    // ENROLLMENT routes (theo EnrollmentController thực tế)
    // ============================================================

    @Test
    void getMyEnrollments_route() throws Exception {
        assertRouteExists(get("/api/v1/enrollments/me"));
    }

    @Test
    void getMyActiveEnrollments_route() throws Exception {
        assertRouteExists(get("/api/v1/enrollments/me/active"));
    }

    @Test
    void getEnrollmentById_route() throws Exception {
        assertRouteExists(get("/api/v1/enrollments/1"));
    }

    @Test
    void enrollInCourse_route() throws Exception {
        assertRouteExists(post("/api/v1/courses/1/enroll"));
    }

    @Test
    void dropEnrollment_route() throws Exception {
        assertRouteExists(post("/api/v1/enrollments/1/drop"));
    }

    @Test
    void dropEnrollmentByCourseId_route() throws Exception {
        assertRouteExists(post("/api/v1/enrollments/by-course/1/drop"));
    }

    // ============================================================
    // PROGRESS routes (theo ProgressController thực tế)
    // ============================================================

    @Test
    void startLesson_route() throws Exception {
        assertRouteExists(post("/api/v1/progress/lessons/1/start"));
    }

    @Test
    void completeLesson_route() throws Exception {
        assertRouteExists(post("/api/v1/progress/lessons/1/complete")
                .contentType("application/json")
                .content("{}"));
    }

    @Test
    void getMyProgressForLesson_route() throws Exception {
        assertRouteExists(get("/api/v1/progress/lessons/1"));
    }

    @Test
    void getMyProgressForCourse_route() throws Exception {
        assertRouteExists(get("/api/v1/progress/courses/1"));
    }
}