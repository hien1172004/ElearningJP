import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useParams, Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import {
  CheckCircle2,
  AlertTriangle,
  Lock,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'
import { Button as AntdButton, Tag } from 'antd'
import {
  BookOutlined,
  ReadOutlined,
  EditOutlined,
  FileTextOutlined,
  AudioOutlined,
} from '@ant-design/icons'
import { PageHeader, LoadingState, StudyTimerTracker, BackButton, ProgressCircle } from '@/components/common'
import { CommentSection } from '@/components/comment'
import { CourseContentUpdateNotice } from '@/components/course'
import { PracticeSheetButton } from '@/components/course/practice-sheet'
import { SaveLessonToNotebookButton } from '@/components/course/save-to-notebook'
import {
  LessonNavSidebar,
  KanjiStudyView,
  VocabStudyView,
  GrammarStudyView,
  SpeakingStudyView,
  QuizStudyView,
} from '@/components/course/lesson-viewer'
import {
  useCourseDetail,
  useLessonDetail,
  useStartLesson,
  useCompleteLesson,
  useUpdateLessonProgress,
} from '@/hooks'
import { useAuthStore } from '@/stores'
import { toSlug } from '@/utils'
import { getLessonDepth, nextLessonState } from '@/utils/lessonNav'
import { canPreviewCourse } from '@/utils/roles'
import { parseCommentFocus } from '@/utils/commentFocus'
import type {
  LessonSectionType,
  LessonKanjiItem,
  LessonVocabularyItem,
  LessonGrammarItem,
  LessonProgressUpdateRequest,
} from '@/types'

type PageKind = 'KANJI' | 'VOCAB' | 'GRAMMAR' | 'SPEAKING' | 'QUIZ'

interface NavItem {
  kind: PageKind
  label: string
  icon: React.ReactNode
  /** Id section (nhóm) của trang — dùng chọn sẵn nhóm khi in phiếu luyện viết. */
  sectionId?: number
  /** items đã load sẵn từ lesson.sections[].xxxItems[] (cho KANJI/VOCAB/GRAMMAR). */
  kanjiItems?: LessonKanjiItem[]
  vocabItems?: LessonVocabularyItem[]
  grammarItems?: LessonGrammarItem[]
}

/** Theo dõi trang nào user đã "xem xong" (đủ điều kiện complete lesson). */
type SectionDoneMap = Partial<Record<PageKind, boolean>>
/** Số mục đã xem theo dạng bài lý thuyết. */
type ViewedMap = Partial<Record<PageKind, number>>
/** % hoàn thành theo dạng bài (0-100). */
type PercentMap = Partial<Record<PageKind, number>>

export const LessonViewerPage: React.FC = () => {
  // URL dạng mới (slug): /learn/:courseSlug/:lessonSlug?courseId=1&lessonId=5
  //   → ưu tiên lấy ID từ query string (chính xác tuyệt đối).
  // URL cũ (numeric): /learn/1/5  → param sẽ là "1" "5"
  //   → tự redirect sang slug-based ngay khi lấy được course + lesson.
  const params = useParams<{ courseSlug?: string; lessonSlug?: string }>()
  const [searchParams] = useSearchParams()
  const qCourseId = searchParams.get('courseId')
  const qLessonId = searchParams.get('lessonId')

  // Nếu param là số (URL cũ) thì dùng luôn làm id, nếu không thì ưu tiên query string
  const cId = Number(
    qCourseId ??
    (params.courseSlug && /^\d+$/.test(params.courseSlug) ? params.courseSlug : ''),
  )
  const lId = Number(
    qLessonId ??
    (params.lessonSlug && /^\d+$/.test(params.lessonSlug) ? params.lessonSlug : ''),
  )

  const navigate = useNavigate()
  const location = useLocation()

  /** Quay lại trang khóa học: lùi lịch sử nếu vào từ trang khóa học, ngược lại mở trang khóa học. */
  const backToCourse = () => {
    const depth = getLessonDepth(location.state)
    const idx = (window.history.state as { idx?: number } | null)?.idx
    if (depth > 0 && typeof idx === 'number' && idx >= depth) navigate(-depth)
    else navigate(`/courses/${cId}`)
  }
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const user = useAuthStore((s) => s.user)

  const { data: course } = useCourseDetail(cId)
  const {
    data: lesson,
    isLoading,
    isError,
    error: lessonError,
  } = useLessonDetail(lId)
  const startLesson = useStartLesson(lId, cId)
  const completeLesson = useCompleteLesson(lId, cId)

  // Nếu URL cũ (numeric) mà đã có data → redirect sang slug-based (replaceState).
  // Nếu URL mới (slug) nhưng thiếu query string → bổ sung query string để các page-level
  // hook (KanjiStudyCard...) có thể gọi lại API detail nếu cần.
  useEffect(() => {
    if (!course || !lesson) return
    if (!Number.isFinite(cId) || !Number.isFinite(lId) || cId <= 0 || lId <= 0) return
    const courseSlug = course.slug || toSlug(course.title)
    const lessonSlug = toSlug(lesson.title)
    const needReplace =
      params.courseSlug !== courseSlug ||
      params.lessonSlug !== lessonSlug ||
      !qCourseId ||
      !qLessonId
    if (needReplace) {
      // Giữ các query param khác (vd `comment`/`reply`/`cpos` khi mở từ thông báo).
      const nextParams = new URLSearchParams(searchParams)
      nextParams.set('courseId', String(cId))
      nextParams.set('lessonId', String(lId))
      navigate(`/learn/${courseSlug}/${lessonSlug}?${nextParams.toString()}`, {
        replace: true,
        state: location.state, // giữ lessonDepth (nút quay lại)
      })
    }
  }, [course, lesson, cId, lId, params.courseSlug, params.lessonSlug, qCourseId, qLessonId, searchParams, navigate])

  // Bình luận cần mở thẳng (từ thông báo) — target luôn là bài học đang xem.
  const commentFocus = parseCommentFocus(searchParams)

  // Tự động start khi đã đăng ký khóa + không có lỗi. Giáo viên/admin xem bài (chưa đăng ký)
  // không gọi /start — BE chỉ ghi tiến độ cho học viên nên sẽ trả 403.
  const isEnrolled = course?.isEnrolled ?? false
  // Admin (mọi khóa) / giáo viên chủ khóa chưa đăng ký → chế độ xem trước:
  // mở mọi phần, không ghi tiến độ, không nộp quiz
  const isPreview = !isEnrolled && canPreviewCourse(user, course?.canManage)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (lId && isAuthenticated && isEnrolled && !isError) {
      startLesson.mutate()
    }
  }, [lId, isAuthenticated, isEnrolled, isError])

  // Build danh sách "trang": Chữ Hán → Từ vựng → Ngữ pháp. Nhiều nhóm cùng loại gộp thành 1 trang
  // (tiến độ lưu theo loại phần), giữ thứ tự nhóm theo orderIndex rồi tới thứ tự mục trong nhóm.
  // Chỉ phụ thuộc các trường nội dung (không phụ thuộc cả `lesson`): lesson detail được tải lại sau mỗi lần
  // lưu tiến độ, React Query giữ nguyên tham chiếu `sections` nên items không đổi → StudyView không nhảy về mục đầu.
  const lessonSections = lesson?.sections
  const lessonSpeaking = lesson?.speakingScenario
  const lessonHasQuiz = lesson?.hasQuiz
  const navItems: NavItem[] = useMemo(() => {
    if (!lessonSections && !lessonSpeaking && !lessonHasQuiz) return []
    const sections = (lessonSections ?? []).slice().sort((a, b) => a.orderIndex - b.orderIndex)
    const kanji: LessonKanjiItem[] = []
    const vocab: LessonVocabularyItem[] = []
    const grammar: LessonGrammarItem[] = []
    let kanjiSectionId: number | undefined
    let vocabSectionId: number | undefined
    for (const sec of sections) {
      if (sec.sectionType === 'KANJI' && sec.kanjiItems?.length) {
        kanjiSectionId ??= sec.id
        kanji.push(...byOrder(sec.kanjiItems))
      } else if (sec.sectionType === 'VOCABULARY' && sec.vocabularyItems?.length) {
        vocabSectionId ??= sec.id
        vocab.push(...byOrder(sec.vocabularyItems))
      } else if (sec.sectionType === 'GRAMMAR' && sec.grammarItems?.length) {
        grammar.push(...byOrder(sec.grammarItems))
      }
    }
    const items: NavItem[] = []
    if (kanji.length > 0) {
      items.push({
        kind: 'KANJI',
        sectionId: kanjiSectionId,
        label: defaultTitleForPage('KANJI'),
        icon: <BookOutlined className="text-base" />,
        kanjiItems: renumber(kanji),
      })
    }
    if (vocab.length > 0) {
      items.push({
        kind: 'VOCAB',
        sectionId: vocabSectionId,
        label: defaultTitleForPage('VOCAB'),
        icon: <ReadOutlined className="text-base" />,
        vocabItems: renumber(vocab),
      })
    }
    if (grammar.length > 0) {
      items.push({
        kind: 'GRAMMAR',
        label: defaultTitleForPage('GRAMMAR'),
        icon: <EditOutlined className="text-base" />,
        grammarItems: renumber(grammar),
      })
    }
    // Luyện nói hội thoại nếu bài học có kịch bản hội thoại
    if (lessonSpeaking) {
      items.push({
        kind: 'SPEAKING',
        label: 'Luyện nói',
        icon: <AudioOutlined className="text-base" />,
      })
    }
    // Test (Quiz) cuối cùng nếu lesson có quiz
    if (lessonHasQuiz) {
      items.push({
        kind: 'QUIZ',
        label: 'Bài kiểm tra',
        icon: <FileTextOutlined className="text-base" />,
      })
    }
    return items
  }, [lessonSections, lessonSpeaking, lessonHasQuiz])

  // Trang hiện tại
  const [pageIdx, setPageIdx] = useState(0)
  // Trạng thái đang trực tiếp luyện nói cùng AI (để tối ưu layout toàn màn hình, chống cuộn 2 thanh scroll)
  const [isSpeakingChatActive, setIsSpeakingChatActive] = useState(false)
  // Reset khi lesson đổi
  useEffect(() => {
    setPageIdx(0)
    setIsSpeakingChatActive(false)
  }, [lId])

  const isCompleted = lesson?.status === 'COMPLETED'
  // Chỉ học viên đã đăng ký mới ghi tiến độ (xem trước của giáo viên/admin thì không)
  const canSaveProgress = isEnrolled && !isPreview
  const updateProgress = useUpdateLessonProgress(lId, cId)

  // Kiểm tra tình trạng kịch bản luyện nói từ backend:
  const speakingList = lesson?.speakingScenarios ?? (lesson?.speakingScenario ? [lesson.speakingScenario] : [])
  const speakingDoneCount = speakingList.filter((s) => s.completed).length

  // Số mục đã xem trong phiên hiện tại (hiện ngay, không chờ server) — gộp với giá trị server bằng max
  const [localViewed, setLocalViewed] = useState<ViewedMap>({})
  // Quiz đạt trong phiên; khi tải lại, server đã ghi bài COMPLETED nên không cần lưu riêng
  const [quizPassed, setQuizPassed] = useState(false)
  useEffect(() => {
    setLocalViewed({})
    setQuizPassed(false)
  }, [lId])

  const markViewed = (kind: PageKind, count: number) => {
    setLocalViewed((prev) => ((prev[kind] ?? 0) >= count ? prev : { ...prev, [kind]: count }))
  }

  /** % từng dạng bài: Chữ Hán/Từ vựng/Ngữ pháp theo số mục đã xem, Luyện nói theo số tình huống xong, Quiz 0/100. */
  const pagePercent = useMemo<PercentMap>(() => {
    const res: PercentMap = {}
    for (const it of navItems) {
      const sectionType = SECTION_TYPE_OF[it.kind]
      if (sectionType) {
        const viewed = Math.max(lesson?.sectionProgress?.[sectionType] ?? 0, localViewed[it.kind] ?? 0)
        res[it.kind] = isCompleted ? 100 : toPercent(viewed, pageItemCount(it))
      } else if (it.kind === 'SPEAKING') {
        res.SPEAKING = toPercent(speakingDoneCount, speakingList.length)
      } else if (it.kind === 'QUIZ') {
        res.QUIZ = isCompleted || quizPassed ? 100 : 0
      }
    }
    return res
  }, [navItems, lesson?.sectionProgress, localViewed, isCompleted, speakingDoneCount, speakingList.length, quizPassed])

  // Dạng bài "xong" = đạt 100%
  const effectiveSectionDone = useMemo<SectionDoneMap>(
    () => Object.fromEntries(navItems.map((it) => [it.kind, (pagePercent[it.kind] ?? 0) >= 100])),
    [navItems, pagePercent],
  )

  // % cả bài = trung bình các dạng bài (làm tròn xuống để chưa xong hết thì không hiện 100%)
  const lessonPercent = isCompleted
    ? 100
    : navItems.length === 0
      ? 0
      : Math.floor(navItems.reduce((sum, it) => sum + (pagePercent[it.kind] ?? 0), 0) / navItems.length)
  const savedLessonPercent = course?.lessons?.find((l) => l.id === lId)?.progressPercent ?? 0

  // Gửi lên server khi số mục đã xem hoặc % cả bài vượt giá trị đã lưu (mỗi lần 1 request;
  // request kế tiếp chạy sau khi lesson detail được tải lại)
  const sentRef = useRef<{ viewed: ViewedMap; percent: number }>({ viewed: {}, percent: 0 })
  useEffect(() => {
    sentRef.current = { viewed: {}, percent: 0 }
  }, [lId])
  useEffect(() => {
    if (!canSaveProgress || !lesson) return
    const sent = sentRef.current
    const changed = navItems.find((it) => {
      const sectionType = SECTION_TYPE_OF[it.kind]
      const viewed = localViewed[it.kind] ?? 0
      return !!sectionType && viewed > (lesson.sectionProgress?.[sectionType] ?? 0) && viewed > (sent.viewed[it.kind] ?? 0)
    })
    const percent = !isCompleted && lessonPercent > savedLessonPercent && lessonPercent > sent.percent ? lessonPercent : undefined
    if (!changed && percent === undefined) return
    const payload: LessonProgressUpdateRequest = { lessonPercent: percent }
    if (changed) {
      const viewed = localViewed[changed.kind] ?? 0
      sent.viewed[changed.kind] = viewed
      payload.sectionType = SECTION_TYPE_OF[changed.kind]
      payload.viewedCount = viewed
    }
    if (percent !== undefined) sent.percent = percent
    updateProgress.mutate(payload)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canSaveProgress, lesson, navItems, localViewed, lessonPercent, savedLessonPercent, isCompleted])

  const safePageIdx = Math.max(0, Math.min(pageIdx, Math.max(0, navItems.length - 1)))
  const currentPage = navItems[safePageIdx]

  /**
   * Bài học có thể hoàn thành khi:
   * - Có ít nhất 1 trang (navItems.length > 0)
   * - Tất cả các trang đều đạt 100% (xem hết mục, xong mọi tình huống nói, đạt passingScore với Quiz).
   */
  const isLessonCompleteable = navItems.length > 0 && navItems.every((it) => effectiveSectionDone[it.kind])

  // Tự động ghi nhận hoàn thành bài học vào Database khi người học hoàn thành tất cả các phần
  useEffect(() => {
    if (isLessonCompleteable && !isCompleted && !isPreview && !completeLesson.isPending) {
      completeLesson.mutate()
    }
  }, [isLessonCompleteable, isCompleted, isPreview, completeLesson])

  /**
   * Cơ chế mở khóa tuần tự linh hoạt:
   * Dựa theo THỨ TỰ THỰC TẾ của các phần trong bài học (navItems):
   * - Dạng bài đầu tiên luôn luôn mở.
   * - Mỗi dạng bài tiếp theo sẽ mở khi dạng bài liền trước nó đã hoàn thành.
   * - Khi bài học đã COMPLETED hoặc chế độ xem trước (preview) -> Mở tất cả 100%.
   */
  const lockedMap = useMemo<Record<PageKind, boolean>>(() => {
    const res: Record<PageKind, boolean> = {
      KANJI: false,
      VOCAB: false,
      GRAMMAR: false,
      SPEAKING: false,
      QUIZ: false,
    }
    if (isPreview || isCompleted) return res

    let prevDone = true
    for (let i = 0; i < navItems.length; i++) {
      const it = navItems[i]
      if (i === 0) {
        res[it.kind] = false
      } else {
        res[it.kind] = !prevDone
      }
      if (!effectiveSectionDone[it.kind]) {
        prevDone = false
      }
    }
    return res
  }, [navItems, effectiveSectionDone, isPreview, isCompleted])

  // Defensive: nếu trang hiện tại bị khóa (vd: pageIdx trỏ tới QUIZ mà chưa đủ điều kiện)
  // → tự động nhảy về trang đầu tiên KHÔNG bị khóa.
  useEffect(() => {
    if (navItems.length === 0) return
    const current = navItems[safePageIdx]
    if (current && lockedMap[current.kind]) {
      const firstUnlocked = navItems.findIndex((it) => !lockedMap[it.kind])
      if (firstUnlocked >= 0) setPageIdx(firstUnlocked)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lockedMap, navItems])

  // ---------- Error / loading states ----------
  if (isLoading) return <LoadingState message="Đang tải bài học..." fullHeight />

  if (isError) {
    const axiosErr = lessonError as any
    const status = axiosErr?.response?.status
    const code = axiosErr?.response?.data?.code
    const message = axiosErr?.response?.data?.message

    let title = 'Không thể tải bài học'
    let description = message ?? 'Đã xảy ra lỗi không xác định.'
    let action: React.ReactNode = null

    if (status === 401) {
      title = 'Phiên đăng nhập hết hạn'
      description = 'Vui lòng đăng nhập lại để tiếp tục.'
      action = (
        <Link to={`/login?redirect=/learn/${cId}/${lId}`}>
          <AntdButton type="primary" className="!rounded-md !font-semibold">
            Đăng nhập lại
          </AntdButton>
        </Link>
      )
    } else if (status === 403 || code === 2002) {
      title = 'Bạn chưa đăng ký khóa học này'
      description = 'Vui lòng đăng ký khóa học trước khi xem bài học.'
      action = (
        <Link to={`/courses/${cId}`}>
          <AntdButton type="primary" className="!rounded-md !font-semibold">
            Quay lại trang khóa học
          </AntdButton>
        </Link>
      )
    } else if (status === 404) {
      title = 'Không tìm thấy bài học'
      description = 'Bài học này có thể đã bị xóa hoặc không tồn tại.'
    } else {
      description = message || 'Máy chủ gặp sự cố. Vui lòng thử lại sau.'
    }

    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md mx-auto px-4">
          <div className="w-16 h-16 rounded-md bg-rose-50 text-rose-500 flex items-center justify-center mx-auto">
            {status === 401 ? <Lock className="w-8 h-8" /> : <AlertTriangle className="w-8 h-8" />}
          </div>
          <h2 className="text-xl font-bold text-gray-900">{title}</h2>
          <p className="text-sm text-gray-500">{description}</p>
          {action && <div className="pt-2">{action}</div>}
          <Link to="/courses" className="inline-block mt-2 text-sm text-indigo-600 hover:underline">
            ← Danh sách khóa học
          </Link>
        </div>
      </div>
    )
  }

  if (!lesson) {
    return (
      <div className="text-center py-20 space-y-4">
        <p className="text-rose-600 font-semibold">Không tìm thấy bài học này.</p>
        <Link to="/courses" className="text-indigo-600 underline text-sm">
          Quay lại danh sách khóa học
        </Link>
      </div>
    )
  }

  const lessons = course?.lessons ?? []
  const currentIdx = lessons.findIndex((l) => l.id === lId)
  const nextLesson =
    currentIdx >= 0 && currentIdx < lessons.length - 1 ? lessons[currentIdx + 1] : null

  const goPrevPage = () => setPageIdx((p) => Math.max(0, p - 1))
  const goNextPage = () => {
    // Đánh dấu dạng bài hiện tại đã hoàn thành khi người học nhấn Next (chỉ cho lý thuyết KANJI/VOCAB/GRAMMAR; SPEAKING và QUIZ phải đạt điều kiện riêng)
    if (currentPage && SECTION_TYPE_OF[currentPage.kind]) {
      markViewed(currentPage.kind, pageItemCount(currentPage))
    }
    if (safePageIdx < navItems.length - 1) {
      // Tìm trang kế tiếp KHÔNG bị khóa (bỏ qua các trang bị khóa)
      for (let i = safePageIdx + 1; i < navItems.length; i++) {
        if (!lockedMap[navItems[i].kind]) {
          setPageIdx(i)
          return
        }
      }
    } else if (nextLesson) {
      // Học xong bài → qua bài kế tiếp
      const cSlug = course?.slug ?? String(cId)
      navigate(
        `/learn/${cSlug}/${toSlug(nextLesson.title)}?courseId=${cId}&lessonId=${nextLesson.id}`,
        { state: nextLessonState(location.state) },
      )
    }
  }

  const handleComplete = () => {
    if (isPreview) return // xem trước: không ghi tiến độ
    if (!isLessonCompleteable && !isCompleted) return
    completeLesson.mutate()
  }

  // Empty state khi lesson không có page nào
  // NOTE: Nếu user thấy màn hình này → lesson.sections rỗng hoặc tất cả sections không có items.
  //       Bật debugMode=true để xem raw data từ API.
  const debugMode = false // ← đổi thành true để debug
  if (navItems.length === 0) {
    const debugSections = lesson?.sections ?? []
    return (
      <div className="pb-12">
        <PageHeader
          badge={course?.jlptLevel ? `JLPT ${course.jlptLevel}` : undefined}
          title={
            <div className="text-left md:text-center">
              <p className="text-sm text-nibi font-normal mb-1">{course?.title}</p>
              <div className="flex items-center gap-3 flex-wrap justify-start md:justify-center">
                {course?.jlptLevel && (
                  <Tag color="purple" className="!m-0 font-bold text-xs">
                    JLPT {course.jlptLevel}
                  </Tag>
                )}
                <p className="text-xl md:text-2xl font-extrabold text-white">
                  {lesson.title}
                </p>
              </div>
            </div>
          }
          variant="indigo"
          actions={
            isPreview ? (
              <Tag color="gold" className="!m-0 font-bold">Chế độ xem trước</Tag>
            ) : isCompleted ? (
              <div className="inline-flex items-center gap-2 px-4 py-2 rounded-md bg-emerald-500/20 border border-emerald-400/30 text-emerald-200 text-sm font-bold">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                Đã hoàn thành
              </div>
            ) : (
              <AntdButton
                type="primary"
                size="large"
                icon={<CheckCircle2 className="w-5 h-5" />}
                loading={completeLesson.isPending}
                onClick={handleComplete}
                disabled={!isLessonCompleteable}
                title={
                  isLessonCompleteable
                    ? 'Hoàn thành bài học'
                    : 'Hoàn thành tất cả phần học để mở khóa'
                }
                className="!rounded-md !font-bold"
              >
                Hoàn thành bài học
              </AntdButton>
            )
          }
        />
        <div className="max-w-3xl mx-auto px-4 py-12 text-center space-y-3">
          <p className="text-base text-gray-500">Bài học này chưa có nội dung.</p>
          <p className="text-xs text-gray-400">Có thể lesson chưa được giáo viên thêm sections hoặc sections chưa có items.</p>

          {debugMode && (
            <div className="mt-6 text-left bg-gray-900 text-green-400 rounded-md p-4 text-xs font-mono overflow-auto overscroll-contain max-h-96">
              <p className="font-bold text-yellow-400 mb-2">🔍 DEBUG — lesson.sections:</p>
              <pre>{JSON.stringify(debugSections, null, 2)}</pre>
              <p className="font-bold text-yellow-400 mt-4 mb-2">🔍 navItems computed:</p>
              <pre>{JSON.stringify(navItems, null, 2)}</pre>
            </div>
          )}
        </div>
      </div>
    )
  }

  // Trang tiếp theo KHÔNG bị khóa (để điều hướng nút Next)
  const nextUnlockedIdx = (() => {
    for (let i = safePageIdx + 1; i < navItems.length; i++) {
      if (!lockedMap[navItems[i].kind]) return i
    }
    return -1
  })()

  // Tên nhãn cho nút "tiếp theo" dựa theo page
  const nextButtonLabel = (() => {
    if (nextUnlockedIdx >= 0) {
      return `Tiếp: ${navItems[nextUnlockedIdx].label}`
    }
    if (nextLesson) return 'Bài tiếp theo'
    return 'Hoàn thành khóa học'
  })()

  return (
    <div className="pb-12">
      {/* Đếm thời gian học — component ẩn, không ảnh hưởng layout */}
      <StudyTimerTracker />
      {/* Khóa vừa có phiên bản mới trong lúc đang học → nhắc tải lại */}
      <CourseContentUpdateNotice courseId={cId} audience="learner" />
      {/* Vào từ trang khóa học → lùi đúng số bài đã đi để về trang khóa học cũ (giữ lịch sử phía trước);
          vào thẳng (thông báo, link…) → mở trang khóa học */}
      <BackButton onClick={backToCourse} label="Quay lại khóa học" className="mb-2" />
      {/* Header */}
      <PageHeader
        title={
          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3">
            <div className="text-left">
              <p className="text-lg sm:text-xl font-bold text-gray-900">{course?.title}</p>
              <p className="text-base text-gray-600">{lesson.title}</p>
            </div>
          </div>
        }
        actions={
          <div className="flex items-center gap-2">
            {isPreview && (
              <Tag color="gold" className="!m-0 font-bold text-sm !px-3 !py-0.5">
                Chế độ xem trước
              </Tag>
            )}
            {course?.jlptLevel && (
              <Tag color="purple" className="!m-0 font-bold text-sm !px-3 !py-0.5">
                JLPT {course.jlptLevel}
              </Tag>
            )}
          </div>
        }
        variant="indigo"
      />

      <div className="max-w-[1650px] mx-auto px-3 sm:px-4 lg:px-5 py-6">
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Sidebar cố định khi cuộn trang (Sticky) — nằm dưới TopBar, cuộn riêng nếu cao hơn màn hình */}
          <div className="lg:w-72 xl:w-80 shrink-0 lg:sticky-panel lg:max-h-[calc(100vh-var(--topbar-h)-2*var(--sticky-gap))] lg:overflow-y-auto overscroll-contain self-start space-y-3">
            <LessonNavSidebar
              courseId={cId}
              courseSlug={course?.slug ?? ''}
              lessons={lessons}
              currentLessonId={lId}
              isEnrolled={isEnrolled || isPreview}
              showProgress={canSaveProgress}
            />
            {/* Page navigator cho 4 dạng bài */}
            <PageNavigator
              items={navItems}
              currentIdx={safePageIdx}
              onSelect={setPageIdx}
              doneMap={effectiveSectionDone}
              percentMap={pagePercent}
              showProgress={canSaveProgress}
              lockedMap={lockedMap}
            />
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="bg-surface rounded-md border border-gray-200/80 shadow-xs p-6 md:p-8">
              {(currentPage?.kind === 'KANJI' || currentPage?.kind === 'VOCAB' || currentPage?.kind === 'GRAMMAR') &&
                (isEnrolled || isPreview) && (
                  <div className="mb-4 flex flex-wrap justify-end gap-2">
                    {/* Lưu kiến thức của bài vào sổ tay — chọn sẵn nhóm đang xem */}
                    <SaveLessonToNotebookButton
                      lessonId={lId}
                      lessonTitle={lesson.title}
                      sections={lesson.sections ?? []}
                      initialSectionId={currentPage.sectionId}
                    />
                    {currentPage.kind !== 'GRAMMAR' && (
                      <PracticeSheetButton
                        lessonId={lId}
                        lessonTitle={lesson.title}
                        type={currentPage.kind === 'KANJI' ? 'KANJI' : 'VOCABULARY'}
                        sections={lesson.sections ?? []}
                        initialSectionId={currentPage.sectionId}
                        size="middle"
                      />
                    )}
                  </div>
                )}
              {currentPage?.kind === 'KANJI' && (
                <KanjiStudyView
                  items={currentPage.kanjiItems ?? []}
                  lessonId={lId || undefined}
                  onViewed={(n) => markViewed('KANJI', n)}
                />
              )}
              {currentPage?.kind === 'VOCAB' && (
                <VocabStudyView
                  items={currentPage.vocabItems ?? []}
                  lessonId={lId || undefined}
                  onViewed={(n) => markViewed('VOCAB', n)}
                />
              )}
              {currentPage?.kind === 'GRAMMAR' && (
                <GrammarStudyView
                  items={currentPage.grammarItems ?? []}
                  lessonId={lId || undefined}
                  onViewed={(n) => markViewed('GRAMMAR', n)}
                />
              )}
              {currentPage?.kind === 'SPEAKING' && ((lesson.speakingScenarios && lesson.speakingScenarios.length > 0) || lesson.speakingScenario) && (
                <SpeakingStudyView
                  lessonId={lId}
                  scenario={lesson.speakingScenario || undefined}
                  scenarios={lesson.speakingScenarios}
                  previewMode={isPreview}
                  onChatModeChange={setIsSpeakingChatActive}
                />
              )}
              {currentPage?.kind === 'QUIZ' && (
                <QuizStudyView
                  lessonId={lId}
                  previewMode={isPreview}
                  onComplete={handleComplete}
                  onSectionDone={() => setQuizPassed(true)}
                />
              )}
              {debugMode && (
                <div className="mt-6 bg-gray-900 text-green-400 rounded-md p-4 text-xs font-mono overflow-auto overscroll-contain max-h-48">
                  <p className="font-bold text-yellow-400 mb-2">🔍 DEBUG — currentPage:</p>
                  <pre>{JSON.stringify(currentPage, null, 2)}</pre>
                </div>
              )}
            </div>

            {/* Footer: Back / Next (ẩn khi đang ở phòng luyện nói trực tiếp cùng AI để tránh xuất hiện 2 thanh cuộn) */}
            {!isSpeakingChatActive && (
              <div className="mt-6 flex items-center justify-between gap-3 bg-surface rounded-md border border-gray-200/80 shadow-xs p-4">
                <button
                  type="button"
                  onClick={goPrevPage}
                  disabled={safePageIdx <= 0}
                  className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-md border border-gray-200 bg-surface text-sm font-semibold text-gray-700 hover:border-indigo-300 hover:text-indigo-600 transition disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-4 h-4" />
                  Quay lại
                </button>
                <AntdButton
                  type="primary"
                  onClick={goNextPage}
                  disabled={nextUnlockedIdx < 0 && !nextLesson}
                  title={
                    nextUnlockedIdx < 0 && !nextLesson
                      ? 'Hoàn thành tất cả phần học để tiếp tục'
                      : undefined
                  }
                  className="!rounded-md !font-semibold"
                >
                  {nextButtonLabel}
                  <ChevronRight className="w-4 h-4 ml-1" />
                </AntdButton>
              </div>
            )}

            {/* Bình luận bài học (ẩn khi đang ở phòng luyện nói trực tiếp cùng AI) */}
            {lId > 0 && !isSpeakingChatActive && (
              <div className="mt-6">
                <CommentSection targetType="LESSON" targetId={lId} focus={commentFocus} />
              </div>
            )}


          </div>
        </div>
      </div>
    </div>
  )
}

// ============================================================================
// Helpers
// ============================================================================
/** Dạng bài lý thuyết ↔ loại phần lưu trên server (SPEAKING/QUIZ không lưu theo mục). */
const SECTION_TYPE_OF: Partial<Record<PageKind, LessonSectionType>> = {
  KANJI: 'KANJI',
  VOCAB: 'VOCABULARY',
  GRAMMAR: 'GRAMMAR',
}

function pageItemCount(it: NavItem): number {
  return it.kanjiItems?.length ?? it.vocabItems?.length ?? it.grammarItems?.length ?? 0
}

/** Làm tròn xuống: chỉ hiện 100% khi đã xong thật. */
function toPercent(done: number, total: number): number {
  return total > 0 ? Math.min(100, Math.floor((done * 100) / total)) : 0
}

function byOrder<T extends { orderIndex?: number }>(list: T[]): T[] {
  return [...list].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
}

/** Đánh lại orderIndex sau khi gộp nhóm — các StudyView tự sắp theo orderIndex. */
function renumber<T extends { orderIndex?: number }>(list: T[]): T[] {
  return list.map((it, i) => ({ ...it, orderIndex: i }))
}

function defaultTitleForPage(kind: PageKind): string {
  switch (kind) {
    case 'KANJI':
      return 'Chữ Hán'
    case 'VOCAB':
      return 'Từ vựng'
    case 'GRAMMAR':
      return 'Ngữ pháp'
    case 'SPEAKING':
      return 'Luyện nói'
    case 'QUIZ':
      return 'Bài kiểm tra'
  }
}

// ============================================================================
// PageNavigator (sidebar bên dưới LessonNavSidebar)
// ============================================================================
interface PageNavigatorProps {
  items: NavItem[]
  currentIdx: number
  onSelect: (idx: number) => void
  doneMap: SectionDoneMap
  percentMap: PercentMap
  /** Hiện vòng tròn % (chỉ học viên đã đăng ký; xem trước thì giữ icon cũ). */
  showProgress: boolean
  lockedMap: Record<PageKind, boolean>
}
const PageNavigator: React.FC<PageNavigatorProps> = ({
  items,
  currentIdx,
  onSelect,
  doneMap,
  percentMap,
  showProgress,
  lockedMap,
}) => {
  if (items.length === 0) return null
  return (
    <div className="mt-3 bg-surface rounded-md border border-gray-200/80 shadow-xs p-3 space-y-1.5">
      <p className="text-xs font-bold text-gray-500 uppercase tracking-wider px-2 py-1">
        Các dạng bài
      </p>
      {items.map((it, idx) => {
        const active = idx === currentIdx
        const isDone = !!doneMap[it.kind]
        const isLocked = !!lockedMap[it.kind]
        // Label: nếu là QUIZ chưa đạt → "Bài kiểm tra (chưa đạt)"
        const displayLabel = it.kind === 'QUIZ' && !isDone ? 'Bài kiểm tra (chưa đạt)' : it.label
        return (
          <button
            key={`${it.kind}-${idx}`}
            type="button"
            disabled={isLocked}
            onClick={() => {
              if (isLocked) return
              onSelect(idx)
            }}
            title={isLocked ? 'Hoàn thành dạng bài trước để mở khóa' : undefined}
            className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-md text-left text-sm transition ${isLocked
                ? 'bg-gray-50 border border-gray-100 text-gray-400 cursor-not-allowed'
                : active
                  ? 'bg-indigo-50 border border-indigo-200 text-indigo-800 font-semibold'
                  : isDone
                    ? 'bg-emerald-50/60 border border-emerald-100 text-emerald-800 hover:bg-emerald-50'
                    : 'border border-transparent text-gray-700 hover:bg-gray-50'
              }`}
          >
            <span className="text-base w-6 shrink-0 text-center flex items-center justify-center">
              {isLocked ? <Lock className="w-4 h-4" /> : it.icon}
            </span>
            <span className="flex-1 truncate">{displayLabel}</span>
            {isLocked ? (
              <span className="text-[10px] font-bold text-gray-400 uppercase">Khóa</span>
            ) : showProgress ? (
              <ProgressCircle percent={percentMap[it.kind] ?? 0} />
            ) : (
              <>
                {active && !isDone && (
                  <span className="text-[10px] font-bold text-indigo-600">ĐANG HỌC</span>
                )}
                {isDone && <CheckCircle2 className="w-4 h-4 text-emerald-500" />}
              </>
            )}
          </button>
        )
      })}
    </div>
  )
}
