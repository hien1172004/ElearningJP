import React, { useState } from 'react'
import { Plus, ChevronUp, ChevronDown, Pencil, Trash2, BookOpen, List, Layers, Eye } from 'lucide-react'
import { Modal, Form, Input, Select, InputNumber, App } from 'antd'
import { z } from 'zod'
import type {
  LessonSection,
  LessonSectionType,
  LessonSectionCreateRequest,
} from '@/types'
import { useSectionMutations } from '@/hooks'

const SECTION_TYPES: { value: LessonSectionType; label: string }[] = [
  { value: 'VOCABULARY', label: 'Từ vựng (Vocabulary)' },
  { value: 'KANJI', label: 'Chữ Hán (Kanji)' },
  { value: 'GRAMMAR', label: 'Ngữ pháp (Grammar)' },
]

const schema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề').max(200),
  sectionType: z.enum(['KANJI', 'VOCABULARY', 'GRAMMAR']),
  orderIndex: z.number().int().min(1),
})

type FormValues = z.infer<typeof schema>

interface SectionListProps {
  lessonId: number
  sections: LessonSection[]
  onManageItems?: (section: LessonSection) => void
}

export const SectionList: React.FC<SectionListProps> = ({
  lessonId,
  sections,
  onManageItems,
}) => {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingSection, setEditingSection] = useState<LessonSection | null>(null)

  const {
    createSection,
    updateSection,
    deleteSection,
    reorderSections,
  } = useSectionMutations(lessonId)

  const isPending =
    createSection.isPending ||
    updateSection.isPending ||
    deleteSection.isPending ||
    reorderSections.isPending

  const openCreate = () => {
    setEditingSection(null)
    form.resetFields()
    form.setFieldsValue({ orderIndex: sections.length + 1 })
    setIsModalOpen(true)
  }

  const openEdit = (section: LessonSection) => {
    setEditingSection(section)
    form.setFieldsValue({
      title: section.title,
      sectionType: section.sectionType,
      orderIndex: section.orderIndex,
    })
    setIsModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = (await form.validateFields()) as FormValues
      const parsed = schema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      const v = parsed.data
      if (editingSection) {
        await updateSection.mutateAsync({ id: editingSection.id, payload: { title: v.title, orderIndex: v.orderIndex } })
      } else {
        await createSection.mutateAsync({
          sectionType: v.sectionType,
          title: v.title,
          orderIndex: v.orderIndex,
        } as LessonSectionCreateRequest)
      }
      setIsModalOpen(false)
    } catch {
      /* */
    }
  }

  const handleReorder = async (direction: 'up' | 'down', index: number) => {
    const arr = [...sections].sort((a, b) => a.orderIndex - b.orderIndex)
    const targetIndex = direction === 'up' ? index - 1 : index + 1
    if (targetIndex < 0 || targetIndex >= arr.length) return
    const newOrder = [...arr]
    ;[newOrder[index], newOrder[targetIndex]] = [newOrder[targetIndex], newOrder[index]]
    await reorderSections.mutateAsync({
      sectionIds: newOrder.map((s) => s.id),
    })
  }

  const typeBadge = (type: LessonSectionType) => {
    const map: Record<LessonSectionType, string> = {
      VOCABULARY: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      KANJI: 'bg-amber-50 text-amber-700 border-amber-200',
      GRAMMAR: 'bg-purple-50 text-purple-700 border-purple-200',
    }
    const label: Record<LessonSectionType, string> = {
      VOCABULARY: 'Từ vựng',
      KANJI: 'Chữ Hán',
      GRAMMAR: 'Ngữ pháp',
    }
    return (
      <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${map[type]}`}>
        {label[type]}
      </span>
    )
  }

  const sorted = [...(sections ?? [])].sort((a, b) => a.orderIndex - b.orderIndex)

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="font-bold text-sm text-gray-800 flex items-center gap-2">
          <List className="w-4 h-4 text-indigo-600" />
          Mục ({sections?.length ?? 0})
        </h3>
        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-indigo-600 text-white text-xs font-bold hover:bg-indigo-700 transition"
        >
          <Plus className="w-4 h-4" /> Thêm section
        </button>
      </div>

      {sorted.length === 0 && (
        <div className="py-6 text-center text-sm text-gray-400 rounded-md border border-dashed border-gray-200">
          Chưa có section nào. Bấm "Thêm section" để tạo.
        </div>
      )}

      <div className="space-y-2">
        {sorted.map((section, idx) => (
          <div
            key={section.id}
            className="flex items-center gap-3 p-3 bg-white rounded-md border border-gray-200/80 hover:border-indigo-300 transition group"
          >
            <div className="shrink-0 w-8 h-8 rounded bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center">
              {section.orderIndex}
            </div>
            {typeBadge(section.sectionType)}
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-sm text-gray-900 truncate">{section.title}</p>
              <p className="text-xs text-gray-400">
                {section.kanjiItems?.length ?? 0} Kanji, {section.vocabularyItems?.length ?? 0} từ,{' '}
                {section.grammarItems?.length ?? 0} ngữ pháp
              </p>
            </div>
            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition">
              {onManageItems && (
                <button
                  type="button"
                  onClick={() => onManageItems(section)}
                  className="px-2 py-1 text-[11px] font-bold text-gray-400 hover:text-indigo-600 rounded transition inline-flex items-center gap-1"
                  title="Quản lý items trong section này"
                >
                  <Eye className="w-4 h-4" />
                </button>
              )}
              {/* ===== TEMP: disabled reorder up/down ===== */}
              {false && (
                <button type="button" onClick={() => handleReorder('up', idx)} disabled={idx === 0} className="p-1 text-gray-400 hover:text-indigo-600 rounded transition disabled:opacity-30">
                  <ChevronUp className="w-4 h-4" />
                </button>
              )}
              {false && (
                <button type="button" onClick={() => handleReorder('down', idx)} disabled={idx === sorted.length - 1} className="p-1 text-gray-400 hover:text-indigo-600 rounded transition disabled:opacity-30">
                  <ChevronDown className="w-4 h-4" />
                </button>
              )}
              <button type="button" onClick={() => openEdit(section)} className="p-1 text-gray-400 hover:text-indigo-600 rounded transition">
                <Pencil className="w-4 h-4" />
              </button>
              <button type="button" onClick={() => deleteSection.mutate(section.id)} className="p-1 text-gray-400 hover:text-rose-600 rounded transition">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      <Modal
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={handleSubmit}
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            {editingSection ? 'Chỉnh sửa Section' : 'Thêm Section mới'}
          </div>
        }
        okText={editingSection ? 'Lưu' : 'Thêm'}
        cancelText="Huỷ"
        confirmLoading={isPending}
        maskClosable={!isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark={false} className="pt-2">
          {!editingSection && (
            <Form.Item
              name="sectionType"
              label="Loại section"
              rules={[{ required: true, message: 'Vui lòng chọn loại' }]}
            >
              <Select size="large" placeholder="Chọn loại section">
                {SECTION_TYPES.map((t) => (
                  <Select.Option key={t.value} value={t.value}>
                    {t.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}
          <Form.Item
            name="title"
            label="Tiêu đề section"
            rules={[{ required: true, message: 'Vui lòng nhập tiêu đề' }]}
          >
            <Input size="large" placeholder="VD: Từ vựng chủ đề Gia đình" />
          </Form.Item>
          <Form.Item name="orderIndex" label="Thứ tự" rules={[{ required: true, message: 'Vui lòng nhập thứ tự' }]}>
            <InputNumber min={1} className="!w-full" size="large" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
