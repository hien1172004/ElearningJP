import React, { useState, useEffect } from 'react'
import { useCourseEditLocked } from '@/components/course/edit-lock'
import { useNavigate } from 'react-router-dom'
import {
  Plus,
  Trash2,
  User,
  Users,
  ListChecks,
  Sparkles,
  BookOpen,
  CheckCircle2,
  Eye,
  Edit3,
  MessageSquare,
  Lightbulb,
} from 'lucide-react'
import { Button as AntdButton, Modal, Select, Tabs } from 'antd'
import { toast } from 'sonner'
import { lessonSpeakingService } from '@/services/lessonSpeaking.service'
import { speakingService } from '@/services/speaking.service'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { EmptyState, LoadingState } from '@/components/common'
import type { LessonSpeakingScenario } from '@/types/course.types'
import type { AiScenario } from '@/types/speaking.types'

interface LessonSpeakingPanelProps {
  lessonId: number
  courseId?: number
  courseLevel?: string
}

export const LessonSpeakingPanel: React.FC<LessonSpeakingPanelProps> = ({
  lessonId,
  courseId,
  courseLevel,
}) => {
  // Khóa đang chờ duyệt: tạm khóa các thao tác sửa nội dung
  const locked = useCourseEditLocked()
  const navigate = useNavigate()
  const [speakingList, setSpeakingList] = useState<LessonSpeakingScenario[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false)
  const [allScenarios, setAllScenarios] = useState<AiScenario[]>([])
  const [isLoadingScenarios, setIsLoadingScenarios] = useState(false)
  const [selectedScenarioId, setSelectedScenarioId] = useState<number | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isGenerating, setIsGenerating] = useState(false)
  const [viewingScenario, setViewingScenario] = useState<LessonSpeakingScenario | null>(null)

  // Load danh sách kịch bản hiện tại của lesson
  const loadLessonSpeaking = async () => {
    setIsLoading(true)
    try {
      const data = await lessonSpeakingService.getSpeakingListForLesson(lessonId)
      setSpeakingList(data || [])
    } catch (err) {
      console.error('Lỗi tải kịch bản luyện nói của bài học:', err)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadLessonSpeaking()
  }, [lessonId])

  // AI tự động tạo kịch bản tiếp theo từ từ vựng và ngữ pháp của bài học
  const handleAiGenerate = async () => {
    setIsGenerating(true)
    const toastId = toast.loading('AI đang phân tích bài học để sinh kịch bản hội thoại tiếp theo...')
    try {
      const generated = await lessonSpeakingService.generateSpeakingScenario(lessonId)
      setSpeakingList((prev) => [...prev, generated])
      toast.success('AI đã tạo và thêm kịch bản hội thoại vào bài học thành công!', { id: toastId })
    } catch (err) {
      console.error('Lỗi khi AI sinh kịch bản:', err)
      toast.error('Không thể tạo kịch bản tự động. Vui lòng thử lại.', { id: toastId })
    } finally {
      setIsGenerating(false)
    }
  }

  // Chuyển sang trang tự soạn kịch bản mới riêng cho bài học
  const handleGoToCreateCustomScenario = () => {
    const params = new URLSearchParams()
    params.set('returnLessonId', String(lessonId))
    if (courseId) params.set('courseId', String(courseId))
    if (courseLevel) params.set('level', courseLevel)
    navigate(`/admin/scenarios/new?${params.toString()}`)
  }

  // Chuyển sang trang chỉnh sửa kịch bản
  const handleEditScenario = (scenarioId: number) => {
    const params = new URLSearchParams()
    params.set('returnLessonId', String(lessonId))
    if (courseId) params.set('courseId', String(courseId))
    if (courseLevel) params.set('level', courseLevel)
    navigate(`/admin/scenarios/${scenarioId}?${params.toString()}`)
  }

  // Load danh sách kịch bản để chọn
  const openAssignModal = async () => {
    setIsAssignModalOpen(true)
    setIsLoadingScenarios(true)
    try {
      const scenarios = await speakingService.getScenarios()
      setAllScenarios(scenarios || [])
      if (scenarios && scenarios.length > 0) {
        // Lọc kịch bản chưa được gán vào bài học
        const assignedIds = new Set(speakingList.map((s) => s.scenarioId))
        const unassigned = scenarios.filter((s) => !assignedIds.has(s.id))
        const matched = unassigned.find((s) => s.jlptLevel === courseLevel)
        if (matched) {
          setSelectedScenarioId(matched.id)
        } else if (unassigned.length > 0) {
          setSelectedScenarioId(unassigned[0].id)
        } else {
          setSelectedScenarioId(scenarios[0].id)
        }
      }
    } catch (err) {
      console.error('Lỗi tải danh sách kịch bản:', err)
      toast.error('Không thể tải danh sách kịch bản.')
    } finally {
      setIsLoadingScenarios(false)
    }
  }

  // Lưu gán kịch bản vào bài học
  const handleAssign = async () => {
    if (!selectedScenarioId) {
      toast.error('Vui lòng chọn một kịch bản hội thoại.')
      return
    }

    setIsSubmitting(true)
    try {
      await lessonSpeakingService.assignSpeakingScenario(lessonId, {
        scenarioId: selectedScenarioId,
        minTurns: 3,
        orderIndex: speakingList.length + 1,
      })
      await loadLessonSpeaking()
      setIsAssignModalOpen(false)
      toast.success('Gán kịch bản hội thoại vào bài học thành công!')
    } catch (err) {
      console.error('Lỗi gán kịch bản:', err)
      toast.error('Không thể gán kịch bản vào bài học. Vui lòng thử lại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  // Gỡ 1 kịch bản cụ thể khỏi bài học
  const handleRemove = async (scenarioId: number) => {
    const ok = await confirmDialog({
      title: 'Gỡ đoạn hội thoại này?',
      content: 'Học viên sẽ không còn thấy đoạn hội thoại này trong bài học.',
      okText: 'Gỡ kịch bản',
      cancelText: 'Hủy',
      danger: true,
    })

    if (!ok) return

    try {
      await lessonSpeakingService.removeSpeakingScenario(lessonId, scenarioId)
      setSpeakingList((prev) => prev.filter((s) => s.scenarioId !== scenarioId))
      toast.success('Đã gỡ kịch bản khỏi bài học.')
    } catch (err) {
      console.error('Lỗi gỡ kịch bản:', err)
      toast.error('Không thể gỡ kịch bản. Vui lòng thử lại.')
    }
  }

  if (isLoading) {
    return <LoadingState message="Đang tải danh sách kịch bản luyện nói..." />
  }

  const selectedScenario = allScenarios.find((s) => s.id === selectedScenarioId)

  return (
    <div className="space-y-4">
      {speakingList.length === 0 ? (
        <EmptyState
          title="Bài học chưa có kịch bản hội thoại"
          description="Gán một hoặc nhiều kịch bản hội thoại vào bài học để học viên thực hành phản xạ nói tiếng Nhật ngay sau phần từ vựng và ngữ pháp."
          action={
            <div className="flex flex-wrap items-center justify-center gap-3">
              <AntdButton
                type="primary"
                icon={<Sparkles className="w-4 h-4" />}
                loading={isGenerating}
                disabled={locked}
                onClick={handleAiGenerate}
                className="!rounded-md !font-semibold"
              >
                Tạo kịch bản từ bài học
              </AntdButton>
              <AntdButton
                icon={<BookOpen className="w-4 h-4" />}
                onClick={openAssignModal}
                disabled={locked || isGenerating}
                className="!rounded-md !font-semibold"
              >
                Chọn từ ngân hàng
              </AntdButton>
              <AntdButton
                icon={<Plus className="w-4 h-4" />}
                onClick={handleGoToCreateCustomScenario}
                disabled={locked || isGenerating}
                className="!rounded-md !font-semibold"
              >
                Tự soạn kịch bản mới
              </AntdButton>
            </div>
          }
        />
      ) : (
        <div className="space-y-4">
          {/* Header Action Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 p-4 bg-white rounded-xl border border-slate-200 shadow-2xs">
            <div>
              <h3 className="text-sm font-bold text-sumi flex items-center gap-2">
                <span>Kịch bản hội thoại giao tiếp</span>
                <span className="px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold border border-indigo-200">
                  {speakingList.length} tình huống
                </span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Một bài học có thể gồm nhiều đoạn hội thoại theo các tình huống giao tiếp khác nhau.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <AntdButton
                icon={<Sparkles className="w-3.5 h-3.5 text-indigo-600" />}
                loading={isGenerating}
                disabled={locked}
                onClick={handleAiGenerate}
              >
                Tạo từ bài học
              </AntdButton>
              <AntdButton
                icon={<BookOpen className="w-3.5 h-3.5 text-slate-600" />}
                disabled={locked || isGenerating}
                onClick={openAssignModal}
              >
                Chọn từ ngân hàng
              </AntdButton>
              <AntdButton
                type="primary"
                icon={<Plus className="w-3.5 h-3.5" />}
                disabled={locked || isGenerating}
                onClick={handleGoToCreateCustomScenario}
                className="!font-semibold"
              >
                Soạn kịch bản mới
              </AntdButton>
            </div>
          </div>

          {/* List of Speaking Scenarios */}
          <div className="space-y-3">
            {speakingList.map((speaking, idx) => (
              <div
                key={speaking.id || idx}
                className="bg-white rounded-xl border border-slate-200 p-4 space-y-3 hover:border-indigo-300 transition"
              >
                <div className="flex flex-wrap items-start justify-between gap-3 pb-2.5 border-b border-slate-100">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="px-2 py-0.5 rounded-md text-xs font-bold bg-slate-100 text-slate-700 font-mono">
                        #{speaking.orderIndex || idx + 1}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                        {speaking.jlptLevel || 'JLPT'}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-800 border border-amber-200">
                        {speaking.topicId || 'Hội thoại'}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        {speaking.tasks?.length
                          ? `${speaking.tasks.length} mục tiêu`
                          : 'Hội thoại'}
                      </span>
                    </div>
                    <h4 className="text-base font-bold text-sumi">{speaking.title}</h4>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <AntdButton
                      size="small"
                      icon={<Eye className="w-3.5 h-3.5 text-indigo-600" />}
                      onClick={() => setViewingScenario(speaking)}
                    >
                      Xem nội dung
                    </AntdButton>
                    <AntdButton
                      size="small"
                      icon={<Edit3 className="w-3.5 h-3.5 text-slate-600" />}
                      onClick={() => handleEditScenario(speaking.scenarioId)}
                    >
                      Sửa kịch bản
                    </AntdButton>
                    <AntdButton
                      size="small"
                      danger
                      icon={<Trash2 className="w-3.5 h-3.5" />}
                      disabled={locked || isGenerating}
                      onClick={() => handleRemove(speaking.scenarioId)}
                    >
                      Gỡ khỏi bài
                    </AntdButton>
                  </div>
                </div>

                <p className="text-xs text-slate-600 leading-relaxed">{speaking.description}</p>

                {/* Phân vai nhân vật */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 p-2.5 rounded-lg bg-slate-50 border border-slate-200/70 text-xs">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
                      <User className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <span className="text-slate-500 font-medium">Người học đóng vai:</span>{' '}
                      <span className="font-bold text-sumi">{speaking.userRole}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                      <Users className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <span className="text-slate-500 font-medium">Đối thoại cùng:</span>{' '}
                      <span className="font-bold text-sumi">{speaking.aiRole}</span>
                    </div>
                  </div>
                </div>

                {/* Nhiệm vụ trong hội thoại */}
                {speaking.tasks && speaking.tasks.length > 0 && (
                  <div>
                    <h5 className="text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                      <ListChecks className="w-3.5 h-3.5 text-indigo-600" />
                      Mục tiêu cần đạt ({speaking.tasks.length} nhiệm vụ)
                    </h5>
                    <ul className="space-y-1 text-xs text-slate-700">
                      {speaking.tasks.map((task) => (
                        <li key={task.id} className="flex items-center gap-1.5 p-1.5 rounded-md bg-slate-50">
                          <div className="w-1.5 h-1.5 rounded-full bg-indigo-500 shrink-0" />
                          <span>{task.text}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal chọn kịch bản và cấu hình */}
      <Modal
        title="Thêm kịch bản hội thoại vào bài học"
        open={isAssignModalOpen}
        onCancel={() => setIsAssignModalOpen(false)}
        onOk={handleAssign}
        confirmLoading={isSubmitting}
        okText="Gán vào bài học"
        cancelText="Hủy"
        width={600}
      >
        <div className="space-y-4 py-2">
          {isLoadingScenarios ? (
            <LoadingState message="Đang tải danh sách kịch bản..." />
          ) : (
            <>
              {/* Chọn kịch bản */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-semibold text-slate-700">
                    Kịch bản hội thoại <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    disabled={locked}
                    onClick={handleGoToCreateCustomScenario}
                    className="inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Tự soạn kịch bản mới</span>
                  </button>
                </div>
                <Select
                  showSearch
                  className="w-full"
                  placeholder="Tìm theo tên bài, chủ đề, trình độ..."
                  value={selectedScenarioId}
                  onChange={(val) => setSelectedScenarioId(val)}
                  filterOption={(input, option) =>
                    (String(option?.label ?? '')).toLowerCase().includes(input.toLowerCase())
                  }
                  options={allScenarios.map((s) => ({
                    value: s.id,
                    label: `[${s.jlptLevel}] ${s.title} (${s.topicId})`,
                  }))}
                />
              </div>

              {/* Chi tiết kịch bản được chọn */}
              {selectedScenario && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 space-y-3 text-xs">
                  <div>
                    <div className="flex flex-wrap items-center gap-1.5 mb-1.5">
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                        {selectedScenario.jlptLevel}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-amber-50 text-amber-800 border border-amber-200">
                        {selectedScenario.topicId}
                      </span>
                      {selectedScenario.tasks && selectedScenario.tasks.length > 0 && (
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                          {selectedScenario.tasks.length} mục tiêu
                        </span>
                      )}
                    </div>
                    <h4 className="font-bold text-sumi text-sm leading-snug">{selectedScenario.title}</h4>
                    <p className="text-slate-600 mt-1 leading-relaxed text-[11px]">{selectedScenario.description}</p>
                  </div>

                  {/* Phân vai nhân vật */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-slate-200/60 text-[11px]">
                    <div className="flex items-center gap-2 text-slate-700">
                      <div className="w-5 h-5 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
                        <User className="w-3 h-3" />
                      </div>
                      <span className="truncate">
                        <span className="text-slate-500 font-medium">Người học:</span>{' '}
                        <strong>{selectedScenario.userRole}</strong>
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-700">
                      <div className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                        <Users className="w-3 h-3" />
                      </div>
                      <span className="truncate">
                        <span className="text-slate-500 font-medium">Đối thoại cùng:</span>{' '}
                        <strong>{selectedScenario.aiRole}</strong>
                      </span>
                    </div>
                  </div>

                  {/* Mục tiêu / Nhiệm vụ trong hội thoại */}
                  {selectedScenario.tasks && selectedScenario.tasks.length > 0 && (
                    <div className="pt-2 border-t border-slate-200/60">
                      <div className="flex items-center gap-1.5 font-semibold text-slate-700 mb-1.5 text-[11px]">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Mục tiêu giao tiếp cần hoàn thành ({selectedScenario.tasks.length}):</span>
                      </div>
                      <ul className="space-y-1 text-[11px] text-slate-600 pl-1">
                        {selectedScenario.tasks.map((task, i) => (
                          <li key={task.id || i} className="flex items-start gap-1.5">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400 mt-1.5 shrink-0" />
                            <span>{task.text}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </Modal>

      {/* Modal xem toàn bộ nội dung kịch bản hội thoại */}
      <Modal
        title={
          viewingScenario ? (
            <div className="flex items-center gap-2.5 pr-6">
              <div className="w-8 h-8 rounded-lg bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-600 shrink-0">
                <MessageSquare className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-base font-bold text-sumi leading-snug">Chi tiết kịch bản hội thoại</h3>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    {viewingScenario.jlptLevel || 'JLPT'}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-amber-50 text-amber-800 border border-amber-200">
                    {viewingScenario.topicId}
                  </span>
                </div>
              </div>
            </div>
          ) : null
        }
        open={!!viewingScenario}
        onCancel={() => setViewingScenario(null)}
        width={780}
        footer={
          viewingScenario ? (
            <div className="flex items-center justify-end gap-2 pt-2">
              <AntdButton
                icon={<Edit3 className="w-3.5 h-3.5 text-slate-600" />}
                onClick={() => {
                  const sid = viewingScenario.scenarioId
                  setViewingScenario(null)
                  handleEditScenario(sid)
                }}
              >
                Chỉnh sửa kịch bản
              </AntdButton>
              <AntdButton type="primary" onClick={() => setViewingScenario(null)}>
                Đóng
              </AntdButton>
            </div>
          ) : null
        }
      >
        {viewingScenario && (
          <div className="space-y-4 py-2 max-h-[72vh] overflow-y-auto pr-1">
            {/* Tiêu đề & Bối cảnh */}
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
              <h4 className="text-base font-bold text-sumi">{viewingScenario.title}</h4>
              <p className="text-xs text-slate-600 leading-relaxed">{viewingScenario.description}</p>

              {/* Phân vai */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-slate-200/70 text-xs">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
                    <User className="w-3.5 h-3.5" />
                  </div>
                  <div>
                    <span className="text-slate-500 font-medium">Người học đóng vai:</span>{' '}
                    <span className="font-bold text-sumi">{viewingScenario.userRole}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                    <Users className="w-3.5 h-3.5" />
                  </div>
                  <div>
                    <span className="text-slate-500 font-medium">Đối thoại cùng:</span>{' '}
                    <span className="font-bold text-sumi">{viewingScenario.aiRole}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Tabs chi tiết: Hội thoại mẫu / Từ vựng - Ngữ pháp / Nhiệm vụ - Mẹo */}
            <Tabs
              defaultActiveKey="dialogue"
              items={[
                {
                  key: 'dialogue',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <MessageSquare className="w-3.5 h-3.5" />
                      <span>Hội thoại mẫu ({viewingScenario.sampleDialogue?.length || 0})</span>
                    </span>
                  ),
                  children: (
                    <div className="space-y-3 py-2">
                      {viewingScenario.sampleDialogue && viewingScenario.sampleDialogue.length > 0 ? (
                        <div className="space-y-3 p-3 bg-slate-50/70 rounded-xl border border-slate-200/80">
                          {viewingScenario.sampleDialogue.map((item: any, i: number) => {
                            const isUser = (item.speaker || item.role || '').toUpperCase() === 'USER'
                            const text = item.text || item.ja || ''
                            const vi = item.vi || item.meaning || ''

                            return (
                              <div
                                key={i}
                                className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} space-y-1`}
                              >
                                <span className="text-[11px] font-semibold text-slate-500 px-1">
                                  {isUser
                                    ? `Học viên (${viewingScenario.userRole})`
                                    : `AI (${viewingScenario.aiRole})`}
                                </span>
                                <div
                                  className={`max-w-[85%] rounded-2xl px-4 py-2.5 shadow-2xs ${isUser
                                      ? 'bg-indigo-600 text-white rounded-tr-sm'
                                      : 'bg-white text-slate-900 border border-slate-200/80 rounded-tl-sm'
                                    }`}
                                >
                                  <p className="text-sm font-medium leading-relaxed">{text}</p>
                                  {vi && (
                                    <p
                                      className={`text-xs mt-1.5 pt-1.5 border-t italic leading-relaxed ${isUser
                                          ? 'text-indigo-100 border-indigo-400/40'
                                          : 'text-slate-500 border-slate-100'
                                        }`}
                                    >
                                      {vi}
                                    </p>
                                  )}
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      ) : (
                        <div className="text-center py-8 text-xs text-slate-500 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                          Chưa có câu thoại mẫu nào. Thầy cô có thể nhấn "Chỉnh sửa kịch bản" để soạn các lượt đối thoại mẫu.
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: 'vocab_grammar',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>Từ vựng & Ngữ pháp</span>
                    </span>
                  ),
                  children: (
                    <div className="space-y-4 py-2">
                      {/* Từ vựng */}
                      <div>
                        <h5 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                          <span>Từ vựng trọng tâm ({viewingScenario.vocabulary?.length || 0})</span>
                        </h5>
                        {viewingScenario.vocabulary && viewingScenario.vocabulary.length > 0 ? (
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            {viewingScenario.vocabulary.map((v: any, i: number) => (
                              <div
                                key={i}
                                className="p-2.5 rounded-lg bg-white border border-slate-200 flex flex-col justify-between"
                              >
                                <div className="flex items-baseline gap-2">
                                  <span className="font-bold text-sumi text-sm">{v.word}</span>
                                  {v.reading && (
                                    <span className="text-xs text-indigo-600 font-mono">({v.reading})</span>
                                  )}
                                </div>
                                <span className="text-xs text-slate-600 mt-1">{v.meaning}</span>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-slate-400 italic">Không có từ vựng riêng.</p>
                        )}
                      </div>

                      {/* Ngữ pháp */}
                      <div>
                        <h5 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                          <span>Mẫu ngữ pháp áp dụng ({viewingScenario.grammar?.length || 0})</span>
                        </h5>
                        {viewingScenario.grammar && viewingScenario.grammar.length > 0 ? (
                          <div className="space-y-2">
                            {viewingScenario.grammar.map((g: any, i: number) => (
                              <div key={i} className="p-2.5 rounded-lg bg-white border border-slate-200 space-y-1">
                                <div className="font-bold text-indigo-700 text-xs">{g.pattern}</div>
                                <div className="text-xs text-slate-700">{g.meaning}</div>
                                {g.example && (
                                  <div className="text-[11px] text-slate-500 bg-slate-50 p-1.5 rounded mt-1">
                                    <span className="font-medium">Ví dụ: </span>
                                    <span>{g.example}</span>
                                    {g.exampleMeaning && (
                                      <span className="italic text-slate-400"> — {g.exampleMeaning}</span>
                                    )}
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-slate-400 italic">Không có mẫu ngữ pháp riêng.</p>
                        )}
                      </div>
                    </div>
                  ),
                },
                {
                  key: 'tasks_tips',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <ListChecks className="w-3.5 h-3.5" />
                      <span>Nhiệm vụ & Mẹo</span>
                    </span>
                  ),
                  children: (
                    <div className="space-y-4 py-2">
                      {/* Nhiệm vụ */}
                      <div>
                        <h5 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Mục tiêu cần đạt ({viewingScenario.tasks?.length || 0})</span>
                        </h5>
                        {viewingScenario.tasks && viewingScenario.tasks.length > 0 ? (
                          <ul className="space-y-1.5 text-xs text-slate-700">
                            {viewingScenario.tasks.map((t: any, i: number) => (
                              <li
                                key={t.id ?? i}
                                className="flex items-start gap-2 p-2 rounded-lg bg-slate-50 border border-slate-200/70"
                              >
                                <span className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-[11px] shrink-0 mt-0.5">
                                  {i + 1}
                                </span>
                                <span className="pt-0.5">{t.text}</span>
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="text-xs text-slate-400 italic">Không có nhiệm vụ cụ thể.</p>
                        )}
                      </div>

                      {/* Mẹo */}
                      <div>
                        <h5 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                          <Lightbulb className="w-3.5 h-3.5 text-amber-500" />
                          <span>Mẹo giao tiếp thực tế ({viewingScenario.tips?.length || 0})</span>
                        </h5>
                        {viewingScenario.tips && viewingScenario.tips.length > 0 ? (
                          <ul className="space-y-1.5 text-xs text-slate-700">
                            {viewingScenario.tips.map((tip: string, i: number) => (
                              <li
                                key={i}
                                className="flex items-start gap-2 p-2 rounded-lg bg-amber-50/60 border border-amber-200/70 text-amber-950"
                              >
                                <Lightbulb className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                                <span>{tip}</span>
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="text-xs text-slate-400 italic">Chưa có mẹo giao tiếp.</p>
                        )}
                      </div>
                    </div>
                  ),
                },
              ]}
            />
          </div>
        )}
      </Modal>
    </div>
  )
}

