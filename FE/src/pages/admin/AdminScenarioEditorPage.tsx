import React, { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { MessageSquareText, Plus, Trash2, FileText, Languages, Sparkles } from 'lucide-react'
import { Button as AntdButton, Form, Input, Select, Switch, Modal } from 'antd'
import type { AxiosError } from 'axios'
import { z } from 'zod'
import { toast } from 'sonner'
import { BackButton, LoadingState, EmptyState, MediaUploadField } from '@/components/common'
import { SCENARIO_TOPICS } from '@/components/admin'
import { useAdminScenario, useCreateScenario, useUpdateScenario } from '@/hooks'
import { toScenarioErrorMessage } from '@/hooks/useAdminScenarios'
import { speakingService } from '@/services/speaking.service'
import { lessonSpeakingService } from '@/services/lessonSpeaking.service'
import type {
  AdminScenario,
  AdminScenarioPayload,
  ApiResponse,
  JlptLevel,
  ScenarioSpeaker,
} from '@/types'

const LEVELS: JlptLevel[] = ['N5', 'N4', 'N3', 'N2', 'N1']

// Giới hạn khớp với validation ở BE (AdminScenarioRequest)
const optionalText = (max: number, label: string) =>
  z.string().trim().max(max, `${label} tối đa ${max} ký tự`).optional().or(z.literal(''))

const schema = z.object({
  title: z.string().trim().min(1, 'Vui lòng nhập tiêu đề').max(255, 'Tiêu đề tối đa 255 ký tự'),
  topicId: z.string().min(1, 'Vui lòng chọn chủ đề'),
  jlptLevel: z.enum(['N5', 'N4', 'N3', 'N2', 'N1'], { message: 'Vui lòng chọn cấp độ' }),
  description: z.string().trim().min(1, 'Vui lòng nhập mô tả tình huống').max(2000, 'Mô tả tối đa 2000 ký tự'),
  userRole: z.string().trim().min(1, 'Vui lòng nhập vai của người học').max(100, 'Vai tối đa 100 ký tự'),
  aiRole: z.string().trim().min(1, 'Vui lòng nhập vai của AI').max(100, 'Vai tối đa 100 ký tự'),
  thumbnailUrl: z
    .string()
    .trim()
    .max(500, 'URL ảnh tối đa 500 ký tự')
    .regex(/^$|^https?:\/\/\S+$/, 'URL ảnh không hợp lệ')
    .optional(),
  isPremium: z.boolean().optional(),
  isLessonOnly: z.boolean().optional(),
  tasks: z
    .array(z.object({ text: z.string().trim().min(1, 'Nội dung nhiệm vụ không được để trống').max(300, 'Mỗi nhiệm vụ tối đa 300 ký tự') }))
    .min(1, 'Cần ít nhất 1 nhiệm vụ')
    .max(10, 'Tối đa 10 nhiệm vụ'),
  sampleDialogue: z
    .array(
      z.object({
        speaker: z.enum(['AI', 'USER']),
        text: z.string().trim().min(1, 'Câu thoại không được để trống').max(500, 'Mỗi câu thoại tối đa 500 ký tự'),
        vi: optionalText(500, 'Bản dịch'),
      }),
    )
    .max(30, 'Tối đa 30 câu hội thoại mẫu'),
  vocabulary: z
    .array(
      z.object({
        word: z.string().trim().min(1, 'Từ vựng không được để trống').max(100, 'Từ vựng tối đa 100 ký tự'),
        reading: optionalText(100, 'Cách đọc'),
        meaning: z.string().trim().min(1, 'Nghĩa của từ không được để trống').max(300, 'Nghĩa tối đa 300 ký tự'),
      }),
    )
    .max(50, 'Tối đa 50 từ vựng'),
  grammar: z
    .array(
      z.object({
        pattern: z.string().trim().min(1, 'Mẫu ngữ pháp không được để trống').max(200, 'Mẫu ngữ pháp tối đa 200 ký tự'),
        meaning: z.string().trim().min(1, 'Ý nghĩa ngữ pháp không được để trống').max(300, 'Ý nghĩa tối đa 300 ký tự'),
        example: optionalText(300, 'Ví dụ'),
        exampleMeaning: optionalText(300, 'Nghĩa ví dụ'),
      }),
    )
    .max(20, 'Tối đa 20 mẫu ngữ pháp'),
  tips: z
    .array(z.string().trim().min(1, 'Mẹo không được để trống').max(500, 'Mỗi mẹo tối đa 500 ký tự'))
    .max(10, 'Tối đa 10 mẹo'),
})

type FormValues = z.input<typeof schema>

const EMPTY_FORM: FormValues = {
  title: '',
  topicId: '',
  jlptLevel: 'N5',
  description: '',
  userRole: '',
  aiRole: '',
  thumbnailUrl: '',
  isPremium: false,
  isLessonOnly: false,
  tasks: [{ text: '' }],
  sampleDialogue: [],
  vocabulary: [],
  grammar: [],
  tips: [],
}

/** Dữ liệu cũ có thể dùng role/ja/meaning thay cho speaker/text/vi — chuẩn hoá khi nạp vào form. */
const toFormValues = (s: AdminScenario): FormValues => ({
  title: s.title,
  topicId: s.topicId,
  jlptLevel: s.jlptLevel,
  description: s.description,
  userRole: s.userRole,
  aiRole: s.aiRole,
  thumbnailUrl: s.thumbnailUrl ?? '',
  isPremium: s.isPremium,
  isLessonOnly: s.isLessonOnly ?? false,
  tasks: (s.tasks ?? []).map((t) => ({ text: t.text ?? '' })),
  sampleDialogue: (s.sampleDialogue ?? []).map((d) => {
    const who = (d.speaker ?? d.role ?? '').toUpperCase()
    return {
      speaker: (who === 'USER' ? 'USER' : 'AI') as ScenarioSpeaker,
      text: d.text ?? d.ja ?? '',
      vi: d.vi ?? d.meaning ?? '',
    }
  }),
  vocabulary: (s.vocabulary ?? []).map((v) => ({
    word: v.word ?? '',
    reading: v.reading ?? '',
    meaning: v.meaning ?? '',
  })),
  grammar: (s.grammar ?? []).map((g) => ({
    pattern: g.pattern ?? '',
    meaning: g.meaning ?? '',
    example: g.example ?? '',
    exampleMeaning: g.exampleMeaning ?? '',
  })),
  tips: s.tips ?? [],
})

const emptyToUndefined = (v?: string) => (v && v.trim() ? v.trim() : undefined)

const toPayload = (v: z.output<typeof schema>): AdminScenarioPayload => ({
  title: v.title,
  topicId: v.topicId,
  jlptLevel: v.jlptLevel,
  description: v.description,
  userRole: v.userRole,
  aiRole: v.aiRole,
  thumbnailUrl: emptyToUndefined(v.thumbnailUrl),
  isPremium: !!v.isPremium,
  isLessonOnly: !!v.isLessonOnly,
  tasks: v.tasks,
  sampleDialogue: v.sampleDialogue.map((d) => ({ ...d, vi: emptyToUndefined(d.vi) })),
  vocabulary: v.vocabulary.map((w) => ({ ...w, reading: emptyToUndefined(w.reading) })),
  grammar: v.grammar.map((g) => ({
    ...g,
    example: emptyToUndefined(g.example),
    exampleMeaning: emptyToUndefined(g.exampleMeaning),
  })),
  tips: v.tips,
})

/** Khung một phần của form. */
const Section: React.FC<{
  title: React.ReactNode
  hint?: string
  extra?: React.ReactNode
  children: React.ReactNode
}> = ({ title, hint, extra, children }) => (
  <section className="bg-surface rounded-xl border border-slate-200 p-4 space-y-3">
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
      <div>
        <h2 className="text-sm font-semibold text-slate-800">{title}</h2>
        {hint && <p className="text-xs text-slate-500 mt-0.5">{hint}</p>}
      </div>
      {extra && <div className="flex items-center gap-2">{extra}</div>}
    </div>
    {children}
  </section>
)

const RemoveButton: React.FC<{ onClick: () => void; label: string }> = ({ onClick, label }) => (
  <AntdButton
    type="text"
    danger
    icon={<Trash2 className="w-4 h-4" />}
    onClick={onClick}
    aria-label={label}
    className="shrink-0"
  />
)

/** Hàm phân tích cú pháp kịch bản hội thoại thô dạng text thành danh sách câu thoại. */
function parseDialogueScript(rawText: string): Array<{ speaker: ScenarioSpeaker; text: string; vi?: string }> {
  const lines = rawText.split('\n').map((l) => l.trim()).filter(Boolean)
  const result: Array<{ speaker: ScenarioSpeaker; text: string; vi?: string }> = []

  lines.forEach((line, index) => {
    let speaker: ScenarioSpeaker = index % 2 === 0 ? 'AI' : 'USER'
    let content = line

    // Nhận diện tiền tố: AI:, USER:, A:, B:, Người học:, Bạn:, Maziiko:
    const speakerMatch = content.match(/^(AI|A|BOT|MAZIIKO|USER|B|NGƯỜI HỌC|BẠN|HỌC VIÊN)\s*[:：\-–]\s*(.*)$/i)
    if (speakerMatch) {
      const prefix = speakerMatch[1].toUpperCase()
      if (['AI', 'A', 'BOT', 'MAZIIKO'].includes(prefix)) {
        speaker = 'AI'
      } else {
        speaker = 'USER'
      }
      content = speakerMatch[2].trim()
    }

    // Tách tiếng Nhật và dịch tiếng Việt nếu có dấu | hoặc //
    let text = content
    let vi = ''
    if (content.includes('|')) {
      const parts = content.split('|')
      text = parts[0].trim()
      vi = parts.slice(1).join('|').trim()
    } else if (content.includes('//')) {
      const parts = content.split('//')
      text = parts[0].trim()
      vi = parts.slice(1).join('//').trim()
    }

    if (text) {
      result.push({ speaker, text, vi: vi || undefined })
    }
  })

  return result
}

export const AdminScenarioEditorPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const returnLessonId = searchParams.get('returnLessonId')
  const returnCourseId = searchParams.get('courseId')
  const initialLevel = searchParams.get('level') as JlptLevel | null

  const scenarioId = id && /^\d+$/.test(id) ? Number(id) : null
  const isEdit = scenarioId !== null

  const [form] = Form.useForm<FormValues>()
  const { data: scenario, isLoading, isError, error } = useAdminScenario(scenarioId)
  const createScenario = useCreateScenario()
  const updateScenario = useUpdateScenario(scenarioId ?? 0)
  const saving = createScenario.isPending || updateScenario.isPending

  // State nhập nhanh hội thoại
  const [isQuickModalOpen, setIsQuickModalOpen] = useState(false)
  const [quickScriptText, setQuickScriptText] = useState('')
  const [isTranslatingDialogue, setIsTranslatingDialogue] = useState(false)

  // Áp dụng đoạn script thô vào form
  const handleApplyQuickScript = () => {
    const parsed = parseDialogueScript(quickScriptText)
    if (parsed.length === 0) {
      toast.error('Không tìm thấy câu thoại hợp lệ trong văn bản.')
      return
    }
    if (parsed.length > 30) {
      toast.error('Kịch bản tối đa 30 câu thoại. Vui lòng rút gọn.')
      return
    }

    form.setFieldsValue({ sampleDialogue: parsed })
    setIsQuickModalOpen(false)
    setQuickScriptText('')
    toast.success(`Đã áp dụng thành công ${parsed.length} câu thoại vào kịch bản!`)
  }

  // Dịch tự động tất cả các câu thoại chưa có tiếng Việt
  const handleAutoTranslateAll = async () => {
    const dialogue = (form.getFieldValue('sampleDialogue') as Array<{ speaker: ScenarioSpeaker; text: string; vi?: string }>) || []
    if (dialogue.length === 0) {
      toast.info('Chưa có câu thoại nào để dịch.')
      return
    }

    const untranslatedIndices = dialogue
      .map((d, idx) => ({ idx, text: d.text, vi: d.vi }))
      .filter((d) => d.text && !d.vi)

    if (untranslatedIndices.length === 0) {
      toast.info('Tất cả các câu thoại đều đã có nghĩa tiếng Việt.')
      return
    }

    setIsTranslatingDialogue(true)
    const toastId = toast.loading(`Đang dùng AI dịch tự động ${untranslatedIndices.length} câu thoại...`)
    try {
      const updated = [...dialogue]
      await Promise.all(
        untranslatedIndices.map(async (item) => {
          try {
            const res = await speakingService.translate(item.text)
            updated[item.idx] = { ...updated[item.idx], vi: res.translatedText }
          } catch (e) {
            console.error('Lỗi dịch câu:', item.text, e)
          }
        }),
      )
      form.setFieldsValue({ sampleDialogue: updated })
      toast.success('Dịch tự động thành công!', { id: toastId })
    } catch {
      toast.error('Có lỗi xảy ra khi dịch tự động.', { id: toastId })
    } finally {
      setIsTranslatingDialogue(false)
    }
  }

  // Nạp dữ liệu kịch bản vào form khi mở trang sửa hoặc gán level mặc định khi tạo mới
  useEffect(() => {
    if (scenario) {
      form.setFieldsValue(toFormValues(scenario))
    } else {
      const initVals: Partial<FormValues> = {}
      if (initialLevel && LEVELS.includes(initialLevel)) {
        initVals.jlptLevel = initialLevel
      }
      if (returnLessonId) {
        initVals.isLessonOnly = true
      }
      form.setFieldsValue(initVals)
    }
  }, [scenario, isEdit, initialLevel, returnLessonId, form])

  const handleFinish = (values: FormValues) => {
    const parsed = schema.safeParse(values)
    if (!parsed.success) {
      toast.error('Kịch bản chưa hợp lệ', { description: parsed.error.issues[0]?.message })
      return
    }
    const payload = toPayload(parsed.data)
    if (returnLessonId) {
      payload.isLessonOnly = true
    }
    const onSuccess = async (res: any) => {
      const createdId = res?.data?.id
      if (returnLessonId && createdId) {
        try {
          await lessonSpeakingService.assignSpeakingScenario(Number(returnLessonId), {
            scenarioId: createdId,
            minTurns: 3,
          })
          toast.success('Đã tự động gán kịch bản vào bài học!')
        } catch (e) {
          console.error('Lỗi tự động gán kịch bản vào bài học:', e)
        }
        if (returnCourseId) {
          navigate(`/teacher/courses/${returnCourseId}/lessons/${returnLessonId}?tab=speaking`)
          return
        }
        navigate(-1)
        return
      }
      navigate('/admin/scenarios')
    }
    if (isEdit) updateScenario.mutate(payload, { onSuccess })
    else createScenario.mutate(payload, { onSuccess })
  }

  const backToUrl = returnLessonId && returnCourseId
    ? `/teacher/courses/${returnCourseId}/lessons/${returnLessonId}?tab=speaking`
    : '/admin/scenarios'
  const backLabel = returnLessonId ? 'Quay lại bài học' : 'Quay lại danh sách'
  const backButton = <BackButton to={backToUrl} label={backLabel} />

  if (isEdit && isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-6">
        <LoadingState message="Đang tải kịch bản..." />
      </div>
    )
  }

  if (isEdit && (isError || !scenario)) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-6">
        <EmptyState
          icon={<MessageSquareText className="w-10 h-10 text-rose-400" />}
          title="Không mở được kịch bản"
          description={
            error
              ? toScenarioErrorMessage(error as AxiosError<ApiResponse<unknown>>, 'Vui lòng thử lại.')
              : 'Không tìm thấy kịch bản.'
          }
          action={backButton}
        />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-5">
      <div>{backButton}</div>

      {returnLessonId && (
        <div className="p-3.5 rounded-xl bg-indigo-50 border border-indigo-200 flex items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 text-indigo-900 font-medium">
            <Sparkles className="w-4 h-4 text-indigo-600 shrink-0" />
            <span>
              Đang soạn kịch bản riêng cho <strong>Bài học #{returnLessonId}</strong>. Sau khi hoàn tất lưu, kịch bản sẽ tự động được gán vào bài học của bạn.
            </span>
          </div>
        </div>
      )}

      <Form<FormValues>
        form={form}
        layout="vertical"
        initialValues={initialLevel && LEVELS.includes(initialLevel) ? { ...EMPTY_FORM, jlptLevel: initialLevel } : EMPTY_FORM}
        onFinish={handleFinish}
        className="space-y-4"
      >
        {/* ======================== Thông tin chung ======================== */}
        <Section title="Thông tin chung">
          <Form.Item
            name="title"
            label="Tiêu đề"
            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tiêu đề' }]}
          >
            <Input maxLength={255} placeholder="VD: Gọi món tại quán Ramen (ラーメン屋で注文)" />
          </Form.Item>
          <div className="grid md:grid-cols-2 gap-x-4">
            <Form.Item name="topicId" label="Chủ đề" rules={[{ required: true, message: 'Vui lòng chọn chủ đề' }]}>
              <Select
                placeholder="Chọn chủ đề"
                options={SCENARIO_TOPICS.map((t) => ({ value: t.id, label: t.name }))}
              />
            </Form.Item>
            <Form.Item name="jlptLevel" label="Cấp độ JLPT" rules={[{ required: true, message: 'Vui lòng chọn cấp độ' }]}>
              <Select options={LEVELS.map((l) => ({ value: l, label: l }))} />
            </Form.Item>
            <Form.Item
              name="userRole"
              label="Vai của người học"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập vai của người học' }]}
            >
              <Input maxLength={100} placeholder="VD: Khách hàng" />
            </Form.Item>
            <Form.Item
              name="aiRole"
              label="Vai của AI"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập vai của AI' }]}
            >
              <Input maxLength={100} placeholder="VD: Nhân viên quán Ramen" />
            </Form.Item>
          </div>
          <Form.Item
            name="description"
            label="Mô tả tình huống"
            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập mô tả tình huống' }]}
          >
            <Input.TextArea
              rows={3}
              maxLength={2000}
              showCount
              placeholder="Bối cảnh người học sẽ gặp, VD: Bạn vừa bước vào một quán mì Ramen ở Tokyo..."
            />
          </Form.Item>
          <div className="grid md:grid-cols-[1fr_auto_auto] gap-x-4 items-end">
            <Form.Item name="thumbnailUrl" label="Ảnh bìa">
              <MediaUploadField accept="image" folder="elearningJP/scenarios/thumbnails" />
            </Form.Item>
            <Form.Item
              name="isLessonOnly"
              label="Dành riêng bài học"
              valuePropName="checked"
              tooltip="Nếu bật, kịch bản chỉ dùng trong các bài học, không xuất hiện ở sảnh Luyện hội thoại tự do."
            >
              <Switch />
            </Form.Item>
            <Form.Item name="isPremium" label="Premium" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Section>

        {/* ======================== Nhiệm vụ ======================== */}
        <Section
          title={
            <span>
              Nhiệm vụ <span className="text-rose-500">*</span>
            </span>
          }
          hint="Những việc người học cần hoàn thành trong hội thoại (1–10 nhiệm vụ)."
        >
          <Form.List name="tasks">
            {(fields, { add, remove }) => (
              <div className="space-y-2">
                {fields.map((field, idx) => (
                  <div key={field.key} className="flex items-start gap-2">
                    <span className="mt-1.5 w-6 shrink-0 text-sm text-slate-500 tabular-nums">{idx + 1}.</span>
                    <Form.Item
                      name={[field.name, 'text']}
                      className="flex-1 mb-0"
                      rules={[{ required: true, whitespace: true, message: 'Nhập nội dung nhiệm vụ' }]}
                    >
                      <Input maxLength={300} placeholder="VD: Gọi 1 bát Shoyu Ramen và đồ uống" />
                    </Form.Item>
                    {fields.length > 1 && <RemoveButton onClick={() => remove(field.name)} label="Xóa nhiệm vụ" />}
                  </div>
                ))}
                {fields.length < 10 && (
                  <AntdButton type="dashed" icon={<Plus className="w-4 h-4" />} onClick={() => add({ text: '' })}>
                    Thêm nhiệm vụ
                  </AntdButton>
                )}
              </div>
            )}
          </Form.List>
        </Section>

        {/* ======================== Hội thoại mẫu ======================== */}
        <Section
          title="Hội thoại mẫu"
          hint="Các câu thoại mẫu giữa AI và người học (tối đa 30 câu)."
          extra={
            <div className="flex flex-wrap items-center gap-2">
              <AntdButton
                size="small"
                icon={<FileText className="w-3.5 h-3.5" />}
                onClick={() => setIsQuickModalOpen(true)}
              >
                Nhập nhanh kịch bản
              </AntdButton>
              <AntdButton
                size="small"
                icon={<Languages className="w-3.5 h-3.5" />}
                loading={isTranslatingDialogue}
                onClick={handleAutoTranslateAll}
              >
                Dịch tự động tiếng Việt
              </AntdButton>
            </div>
          }
        >
          <Form.List name="sampleDialogue">
            {(fields, { add, remove }) => (
              <div className="space-y-2">
                {fields.map((field) => (
                  <div key={field.key} className="flex flex-col md:flex-row md:items-start gap-2 border-b border-slate-100 pb-2 md:border-0 md:pb-0">
                    <Form.Item name={[field.name, 'speaker']} className="mb-0 md:w-32">
                      <Select
                        options={[
                          { value: 'AI', label: 'AI' },
                          { value: 'USER', label: 'Người học' },
                        ]}
                      />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'text']}
                      className="flex-1 mb-0"
                      rules={[{ required: true, whitespace: true, message: 'Nhập câu thoại' }]}
                    >
                      <Input maxLength={500} placeholder="Câu tiếng Nhật" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'vi']} className="flex-1 mb-0">
                      <Input maxLength={500} placeholder="Nghĩa tiếng Việt" />
                    </Form.Item>
                    <RemoveButton onClick={() => remove(field.name)} label="Xóa câu thoại" />
                  </div>
                ))}
                {fields.length < 30 && (
                  <AntdButton
                    type="dashed"
                    icon={<Plus className="w-4 h-4" />}
                    onClick={() =>
                      add({ speaker: fields.length % 2 === 0 ? 'AI' : 'USER', text: '', vi: '' })
                    }
                  >
                    Thêm câu thoại
                  </AntdButton>
                )}
              </div>
            )}
          </Form.List>
        </Section>

        {/* ======================== Từ vựng ======================== */}
        <Section title="Từ vựng gợi ý" hint="Tối đa 50 từ.">
          <Form.List name="vocabulary">
            {(fields, { add, remove }) => (
              <div className="space-y-2">
                {fields.map((field) => (
                  <div key={field.key} className="flex flex-col md:flex-row md:items-start gap-2 border-b border-slate-100 pb-2 md:border-0 md:pb-0">
                    <Form.Item
                      name={[field.name, 'word']}
                      className="flex-1 mb-0"
                      rules={[{ required: true, whitespace: true, message: 'Nhập từ vựng' }]}
                    >
                      <Input maxLength={100} placeholder="Từ (VD: 注文)" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'reading']} className="flex-1 mb-0">
                      <Input maxLength={100} placeholder="Cách đọc (VD: ちゅうもん)" />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'meaning']}
                      className="flex-1 mb-0"
                      rules={[{ required: true, whitespace: true, message: 'Nhập nghĩa' }]}
                    >
                      <Input maxLength={300} placeholder="Nghĩa tiếng Việt" />
                    </Form.Item>
                    <RemoveButton onClick={() => remove(field.name)} label="Xóa từ vựng" />
                  </div>
                ))}
                {fields.length < 50 && (
                  <AntdButton
                    type="dashed"
                    icon={<Plus className="w-4 h-4" />}
                    onClick={() => add({ word: '', reading: '', meaning: '' })}
                  >
                    Thêm từ vựng
                  </AntdButton>
                )}
              </div>
            )}
          </Form.List>
        </Section>

        {/* ======================== Ngữ pháp ======================== */}
        <Section title="Ngữ pháp gợi ý" hint="Tối đa 20 mẫu.">
          <Form.List name="grammar">
            {(fields, { add, remove }) => (
              <div className="space-y-3">
                {fields.map((field) => (
                  <div key={field.key} className="flex items-start gap-2 border-b border-slate-100 pb-3">
                    <div className="flex-1 grid md:grid-cols-2 gap-2">
                      <Form.Item
                        name={[field.name, 'pattern']}
                        className="mb-0"
                        rules={[{ required: true, whitespace: true, message: 'Nhập mẫu ngữ pháp' }]}
                      >
                        <Input maxLength={200} placeholder="Mẫu (VD: 〜をください)" />
                      </Form.Item>
                      <Form.Item
                        name={[field.name, 'meaning']}
                        className="mb-0"
                        rules={[{ required: true, whitespace: true, message: 'Nhập ý nghĩa' }]}
                      >
                        <Input maxLength={300} placeholder="Ý nghĩa" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'example']} className="mb-0">
                        <Input maxLength={300} placeholder="Ví dụ" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'exampleMeaning']} className="mb-0">
                        <Input maxLength={300} placeholder="Nghĩa ví dụ" />
                      </Form.Item>
                    </div>
                    <RemoveButton onClick={() => remove(field.name)} label="Xóa mẫu ngữ pháp" />
                  </div>
                ))}
                {fields.length < 20 && (
                  <AntdButton
                    type="dashed"
                    icon={<Plus className="w-4 h-4" />}
                    onClick={() => add({ pattern: '', meaning: '', example: '', exampleMeaning: '' })}
                  >
                    Thêm mẫu ngữ pháp
                  </AntdButton>
                )}
              </div>
            )}
          </Form.List>
        </Section>

        {/* ======================== Mẹo ======================== */}
        <Section title="Mẹo cho người học" hint="Tối đa 10 mẹo.">
          <Form.List name="tips">
            {(fields, { add, remove }) => (
              <div className="space-y-2">
                {fields.map((field) => (
                  <div key={field.key} className="flex items-start gap-2">
                    <Form.Item
                      name={field.name}
                      className="flex-1 mb-0"
                      rules={[{ required: true, whitespace: true, message: 'Nhập nội dung mẹo' }]}
                    >
                      <Input.TextArea autoSize={{ minRows: 1, maxRows: 4 }} maxLength={500} placeholder="VD: Nói 一人です khi đi một mình" />
                    </Form.Item>
                    <RemoveButton onClick={() => remove(field.name)} label="Xóa mẹo" />
                  </div>
                ))}
                {fields.length < 10 && (
                  <AntdButton type="dashed" icon={<Plus className="w-4 h-4" />} onClick={() => add('')}>
                    Thêm mẹo
                  </AntdButton>
                )}
              </div>
            )}
          </Form.List>
        </Section>

        <div className="flex justify-end gap-2 pb-6">
          <AntdButton onClick={() => navigate('/admin/scenarios')} disabled={saving}>
            Huỷ
          </AntdButton>
          <AntdButton type="primary" htmlType="submit" loading={saving}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo kịch bản'}
          </AntdButton>
        </div>
      </Form>

      {/* Modal nhập nhanh kịch bản hội thoại */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-base font-semibold text-slate-800">
            <FileText className="w-5 h-5 text-indigo-600" />
            <span>Nhập nhanh kịch bản hội thoại</span>
          </div>
        }
        open={isQuickModalOpen}
        onCancel={() => setIsQuickModalOpen(false)}
        width={680}
        footer={[
          <AntdButton key="cancel" onClick={() => setIsQuickModalOpen(false)}>
            Huỷ
          </AntdButton>,
          <AntdButton
            key="apply"
            type="primary"
            icon={<Sparkles className="w-4 h-4" />}
            onClick={handleApplyQuickScript}
          >
            Áp dụng vào kịch bản
          </AntdButton>,
        ]}
      >
        <div className="space-y-4 py-2">
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-xs text-slate-600 space-y-1.5">
            <p className="font-semibold text-slate-700">💡 Hướng dẫn định dạng (Hệ thống tự động phân tích):</p>
            <ul className="list-disc list-inside space-y-0.5 text-slate-600">
              <li>
                Dạng chuẩn: <code>AI: [Câu tiếng Nhật] | [Dịch tiếng Việt]</code> hoặc <code>USER: ...</code>
              </li>
              <li>
                Hỗ trợ tiền tố: <code>A:</code> / <code>B:</code>, <code>Bot:</code> / <code>Bạn:</code>
              </li>
              <li>Nếu không có tiền tố người nói, hệ thống sẽ tự xen kẽ <b>AI</b> và <b>Người học</b> theo thứ tự dòng.</li>
              <li>Dấu gạch đứng <code>|</code> hoặc <code>//</code> dùng để phân tách câu tiếng Nhật và dịch tiếng Việt.</li>
            </ul>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-xs font-semibold text-slate-700">Chèn kịch bản mẫu:</span>
              <div className="flex flex-wrap gap-1.5">
                <AntdButton
                  size="small"
                  onClick={() =>
                    setQuickScriptText(
                      `AI: いらっしゃいませ！何名様ですか？ | Xin chào quý khách! Quý khách đi mấy người ạ?\nUSER: 一人です。 | Tôi đi một mình.\nAI: こちらへどうぞ。ご注文はお決まりですか？ | Mời anh vào lối này. Anh đã chọn món chưa ạ?\nUSER: 醤油ラーメンを一つください。 | Cho tôi một bát Shoyu Ramen.\nAI: かしこまりました。大盛りにしますか？ | Vâng tôi hiểu rồi. Anh có muốn làm bát lớn không ạ?\nUSER: 普通でお願いします。 | Cho tôi phần bình thường thôi nhé.`
                    )
                  }
                >
                  N5 Gọi món
                </AntdButton>
                <AntdButton
                  size="small"
                  onClick={() =>
                    setQuickScriptText(
                      `AI: すみません、何かお困りですか？ | Xin lỗi, bạn có cần giúp đỡ gì không?\nUSER: すみません、東京駅行きの電車はどのホームですか？ | Xin lỗi, tàu đi ga Tokyo ở sân ga nào vậy?\nAI: 3番線ですよ。あちらの階段を上がってください。 | Là sân ga số 3 nhé. Bạn hãy lên cầu thang đằng kia.\nUSER: ありがとうございます。次の電車は何時ですか？ | Cảm ơn bạn. Chuyến tàu tiếp theo lúc mấy giờ vậy?\nAI: 10時15分発の快速です。あと5分で来ますよ。 | Là tàu nhanh khởi hành lúc 10h15. Còn 5 phút nữa tàu đến.`
                    )
                  }
                >
                  N4 Hỏi đường
                </AntdButton>
                <AntdButton
                  size="small"
                  onClick={() =>
                    setQuickScriptText(
                      `USER: 部長、今お時間よろしいでしょうか。 | Thưa trưởng phòng, bây giờ anh có tiện không ạ?\nAI: はい、何でしょうか。 | Được chứ, có chuyện gì thế?\nUSER: 実は朝から熱がありまして、病院に行きたいのですが。 | Thật ra từ sáng em bị sốt, muốn xin phép đi bệnh viện khám ạ.\nAI: それはいけませんね。仕事の引き継ぎは大丈夫ですか？ | Thế thì không ổn rồi. Đã bàn giao công việc chưa?\nUSER: 今日の会議の資料は田中さんに引き継ぎました。 | Tài liệu cuộc họp em đã bàn giao cho anh Tanaka rồi ạ.\nAI: 分かりました。無理をしないでゆっくり休んでください。 | Tôi hiểu rồi. Nghỉ ngơi cho lại sức nhé.`
                    )
                  }
                >
                  N3 Xin phép
                </AntdButton>
                <AntdButton
                  size="small"
                  onClick={() =>
                    setQuickScriptText(
                      `AI: 本日は面接にお越しいただき、ありがとうございます。簡単に自己紹介をお願いできますか？ | Cảm ơn bạn đã đến phỏng vấn hôm nay. Bạn có thể giới thiệu ngắn gọn về bản thân không?\nUSER: はじめまして、グエンと申します。大学で情報工学を専攻し、2年間ウェブ開発に携わってまいりました。 | Xin chào, tôi tên là Nguyen. Tôi tốt nghiệp ngành CNTT và đã có 2 năm kinh nghiệm làm phát triển web.\nAI: ありがとうございます。弊社の志望動機についてもお聞かせいただけますでしょうか。 | Cảm ơn bạn. Bạn có thể chia sẻ thêm về lý do ứng tuyển vào công ty chúng tôi được không?\nUSER: 御社の革新的なAI教育プラットフォームに強く共感し、自分の開発スキルを活かして貢献したいと考え応募いたしました。 | Tôi rất ấn tượng với nền tảng giáo dục AI tiên tiến của quý công ty và muốn đem kinh nghiệm lập trình của mình để đóng góp cho sự phát triển của dự án.`
                    )
                  }
                >
                  N2 Phỏng vấn
                </AntdButton>
              </div>
            </div>

            <Input.TextArea
              rows={11}
              value={quickScriptText}
              onChange={(e) => setQuickScriptText(e.target.value)}
              placeholder={`Dán kịch bản thoại tại đây. Ví dụ:\nAI: こんにちは！ | Xin chào!\nUSER: こんにちは、はじめまして。 | Chào bạn, rất vui được gặp bạn.`}
              className="font-mono text-xs"
            />
          </div>
        </div>
      </Modal>
    </div>
  )
}
