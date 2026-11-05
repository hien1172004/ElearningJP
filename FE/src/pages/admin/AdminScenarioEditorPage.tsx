import React, { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { MessageSquareText, Plus, Trash2 } from 'lucide-react'
import { Button as AntdButton, Form, Input, Select, Switch } from 'antd'
import type { AxiosError } from 'axios'
import { z } from 'zod'
import { toast } from 'sonner'
import { BackButton, LoadingState, EmptyState, MediaUploadField } from '@/components/common'
import { SCENARIO_TOPICS } from '@/components/admin'
import { useAdminScenario, useCreateScenario, useUpdateScenario } from '@/hooks'
import { toScenarioErrorMessage } from '@/hooks/useAdminScenarios'
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
const Section: React.FC<{ title: string; hint?: string; children: React.ReactNode }> = ({
  title,
  hint,
  children,
}) => (
  <section className="bg-white rounded-xl border border-slate-200 p-4 space-y-3">
    <div>
      <h2 className="text-sm font-semibold text-slate-800">{title}</h2>
      {hint && <p className="text-xs text-slate-500 mt-0.5">{hint}</p>}
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

export const AdminScenarioEditorPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const scenarioId = id && /^\d+$/.test(id) ? Number(id) : null
  const isEdit = scenarioId !== null

  const [form] = Form.useForm<FormValues>()
  const { data: scenario, isLoading, isError, error } = useAdminScenario(scenarioId)
  const createScenario = useCreateScenario()
  const updateScenario = useUpdateScenario(scenarioId ?? 0)
  const saving = createScenario.isPending || updateScenario.isPending

  // Nạp dữ liệu kịch bản vào form khi mở trang sửa
  useEffect(() => {
    if (scenario) form.setFieldsValue(toFormValues(scenario))
  }, [scenario, form])

  const handleFinish = (values: FormValues) => {
    const parsed = schema.safeParse(values)
    if (!parsed.success) {
      toast.error('Kịch bản chưa hợp lệ', { description: parsed.error.issues[0]?.message })
      return
    }
    const payload = toPayload(parsed.data)
    const onSuccess = () => navigate('/admin/scenarios')
    if (isEdit) updateScenario.mutate(payload, { onSuccess })
    else createScenario.mutate(payload, { onSuccess })
  }

  const backButton = <BackButton to="/admin/scenarios" />

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

      <Form<FormValues>
        form={form}
        layout="vertical"
        initialValues={EMPTY_FORM}
        onFinish={handleFinish}
        requiredMark={false}
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
          <div className="grid md:grid-cols-[1fr_auto] gap-x-4 items-end">
            <Form.Item name="thumbnailUrl" label="Ảnh bìa (không bắt buộc)">
              <MediaUploadField accept="image" folder="elearningJP/scenarios/thumbnails" />
            </Form.Item>
            <Form.Item name="isPremium" label="Premium" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Section>

        {/* ======================== Nhiệm vụ ======================== */}
        <Section title="Nhiệm vụ" hint="Những việc người học cần hoàn thành trong hội thoại (1–10 nhiệm vụ).">
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
        <Section title="Hội thoại mẫu" hint="Các câu thoại mẫu giữa AI và người học (tối đa 30 câu).">
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
                      <Input maxLength={500} placeholder="Nghĩa tiếng Việt (không bắt buộc)" />
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
                        <Input maxLength={300} placeholder="Ví dụ (không bắt buộc)" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'exampleMeaning']} className="mb-0">
                        <Input maxLength={300} placeholder="Nghĩa ví dụ (không bắt buộc)" />
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
    </div>
  )
}
