import React, { useEffect, useMemo, useState } from 'react'
import { Button, Checkbox, Empty, Modal, Tag, Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { Printer } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { practiceSheetService, type MissingStrokeItem, type PracticeSheetType } from '@/services'
import type { ApiResponse, LessonSection } from '@/types'
import { toSlug } from '@/utils'

interface PracticeSheetModalProps {
  open: boolean
  onClose: () => void
  lessonId: number
  lessonTitle?: string
  type: PracticeSheetType
  /** Các nhóm (section) của bài — chỉ dùng nhóm đúng loại {@link type}. */
  sections: LessonSection[]
  /** Nhóm được chọn sẵn khi mở (vd mở từ nút của 1 nhóm); bỏ trống → chọn tất cả. */
  initialSectionId?: number
}

const MAX_ITEMS = 300
const TYPE_LABEL: Record<PracticeSheetType, string> = { KANJI: 'chữ Hán', VOCABULARY: 'từ vựng' }
const groupKey = (sectionId: number) => `g-${sectionId}`

interface GroupItem {
  id: number
  label: string
  sub?: string
}

const getErrorMessage = (err: unknown) =>
  (err as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? 'Lỗi không xác định'

/** Danh sách mục thiếu nét hiển thị trong hộp xác nhận. */
const MissingList: React.FC<{ items: MissingStrokeItem[]; total: number }> = ({ items, total }) => (
  <div className="space-y-2">
    <p className="text-sm text-slate-600">
      {items.length}/{total} mục có ký tự chưa có dữ liệu thứ tự nét. Các ký tự này vẫn được in chữ mẫu và chữ mờ để
      tô theo, nhưng <b>không có hàng thứ tự từng nét</b>.
    </p>
    <div className="max-h-60 overflow-y-auto rounded-md border border-slate-200 divide-y divide-slate-100">
      {items.map((m) => (
        <div key={m.itemId} className="flex items-center justify-between gap-2 px-3 py-1.5 text-sm">
          <span className="min-w-0 truncate">
            <span className="font-semibold text-slate-800">{m.word}</span>
            <span className="ml-2 text-xs text-slate-400">{m.groupTitle}</span>
          </span>
          <span className="flex shrink-0 flex-wrap gap-1">
            {m.missingChars.map((c) => (
              <Tag key={c} color="orange" className="!m-0">
                {c}
              </Tag>
            ))}
          </span>
        </div>
      ))}
    </div>
    <p className="text-sm text-slate-600">Bạn có muốn tiếp tục in không?</p>
  </div>
)

/**
 * Chọn mục Chữ Hán / Từ vựng của bài để in phiếu luyện viết (PDF):
 * in tất cả, chọn theo nhóm (tick nhóm) hoặc chọn từng từ. Trước khi in kiểm tra ký tự thiếu nét và hỏi người dùng.
 */
export const PracticeSheetModal: React.FC<PracticeSheetModalProps> = ({
  open,
  onClose,
  lessonId,
  lessonTitle,
  type,
  sections,
  initialSectionId,
}) => {
  const groups = useMemo(
    () =>
      sections
        .filter((s) => s.sectionType === type)
        .slice()
        .sort((a, b) => a.orderIndex - b.orderIndex)
        .map((s) => ({
          section: s,
          items: (type === 'KANJI'
            ? (s.kanjiItems ?? []).map((k) => ({
                id: k.id,
                label: k.character ?? '',
                sub: [k.hanViet, k.meaningVi].filter(Boolean).join(' · '),
              }))
            : (s.vocabularyItems ?? []).map((v) => ({
                id: v.id,
                label: v.word ?? '',
                sub: [v.displayHiragana ?? v.hiragana, v.displayMeaning ?? v.meaningVi].filter(Boolean).join(' · '),
              }))) as GroupItem[],
        }))
        .filter((g) => g.items.length > 0),
    [sections, type],
  )
  const allIds = useMemo(() => groups.flatMap((g) => g.items.map((i) => i.id)), [groups])

  const [checked, setChecked] = useState<number[]>([])
  const [busy, setBusy] = useState(false)

  // Mỗi lần mở: chọn sẵn nhóm được truyền vào, không có thì chọn tất cả
  useEffect(() => {
    if (!open) return
    const initial = groups.find((g) => g.section.id === initialSectionId)
    setChecked(initial ? initial.items.map((i) => i.id) : allIds)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initialSectionId])

  const treeData: TreeDataNode[] = groups.map((g) => ({
    key: groupKey(g.section.id),
    title: (
      <span className="font-semibold text-slate-700">
        {g.section.title}
        <span className="ml-2 text-xs font-normal text-slate-400">{g.items.length} mục</span>
      </span>
    ),
    children: g.items.map((i) => ({
      key: i.id,
      isLeaf: true,
      title: (
        <span>
          <span className="text-base text-slate-800">{i.label}</span>
          {i.sub && <span className="ml-2 text-xs text-slate-500">{i.sub}</span>}
        </span>
      ),
    })),
  }))

  const allChecked = allIds.length > 0 && checked.length === allIds.length
  const label = TYPE_LABEL[type]

  const handlePrint = async () => {
    // Giữ thứ tự theo bài học (BE cũng sắp lại theo nhóm → thứ tự trong nhóm)
    const itemIds = allIds.filter((id) => checked.includes(id))
    if (itemIds.length === 0) return
    const body = { sectionType: type, itemIds }
    setBusy(true)
    try {
      const res = await practiceSheetService.check(lessonId, body)
      const missing = res.data?.missingItems ?? []
      if (missing.length > 0) {
        const ok = await confirmDialog({
          title: 'Một số mục thiếu dữ liệu thứ tự nét',
          content: <MissingList items={missing} total={res.data?.totalItems ?? itemIds.length} />,
          okText: 'Vẫn in',
          cancelText: 'Huỷ',
        })
        if (!ok) return
      }
      const slug = toSlug(lessonTitle ?? '') || `bai-${lessonId}`
      const prefix = type === 'KANJI' ? 'luyen_viet_chu_han' : 'luyen_viet_tu_vung'
      await practiceSheetService.download(lessonId, body, `${prefix}_${slug}.pdf`)
      toast.success('Đã tạo phiếu luyện viết')
      onClose()
    } catch (e) {
      toast.error('In phiếu luyện viết thất bại', { description: getErrorMessage(e) })
    } finally {
      setBusy(false)
    }
  }

  const tooMany = checked.length > MAX_ITEMS

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={`In phiếu luyện viết ${label}`}
      width={640}
      destroyOnHidden
      footer={
        <div className="flex items-center justify-between gap-2">
          <span className={`text-sm ${tooMany ? 'text-red-500' : 'text-slate-500'}`}>
            Đã chọn {checked.length}/{allIds.length} mục{tooMany ? ` — tối đa ${MAX_ITEMS} mục mỗi lần in` : ''}
          </span>
          <div className="flex gap-2">
            <Button onClick={onClose}>Huỷ</Button>
            <Button
              type="primary"
              icon={<Printer className="h-4 w-4" />}
              loading={busy}
              disabled={checked.length === 0 || tooMany}
              onClick={handlePrint}
            >
              In phiếu
            </Button>
          </div>
        </div>
      }
    >
      {groups.length === 0 ? (
        <Empty description={`Bài học chưa có ${label} để in`} />
      ) : (
        <div className="space-y-3">
          <p className="text-sm text-slate-500">
            Tick tên nhóm để chọn cả nhóm, hoặc chọn từng {label}. Mỗi nhóm được in thành một phần riêng trong file PDF.
          </p>
          <Checkbox
            checked={allChecked}
            indeterminate={checked.length > 0 && !allChecked}
            onChange={(e) => setChecked(e.target.checked ? allIds : [])}
          >
            <span className="font-medium">Chọn tất cả ({allIds.length})</span>
          </Checkbox>
          <div className="max-h-[50vh] overflow-y-auto rounded-md border border-slate-200 p-2">
            <Tree
              checkable
              selectable={false}
              defaultExpandAll
              treeData={treeData}
              checkedKeys={checked}
              onCheck={(keys) => {
                const list = Array.isArray(keys) ? keys : keys.checked
                setChecked(list.filter((k): k is number => typeof k === 'number'))
              }}
            />
          </div>
        </div>
      )}
    </Modal>
  )
}
