import React, { useEffect, useMemo, useState } from 'react'
import { Button, Checkbox, Empty, Modal, Tag, Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { BookmarkPlus } from 'lucide-react'
import type { LessonSection, NotebookSavedIds, SaveLessonItemRef } from '@/types'
import { AddWordToNotebookModal, type LessonSaveTarget } from '@/components/notebook/AddWordToNotebookModal'
import { useNotebookSavedIds } from '@/hooks/useNotebooks'

type SectionType = SaveLessonItemRef['type']

interface SaveLessonToNotebookModalProps {
  open: boolean
  onClose: () => void
  lessonId: number
  lessonTitle?: string
  sections: LessonSection[]
  /** Nhóm được chọn sẵn khi mở (vd mở từ trang của 1 nhóm); bỏ trống → chọn cả bài. */
  initialSectionId?: number
}

const MAX_ITEMS = 500
const SECTION_META: Record<SectionType, { label: string; color: string }> = {
  KANJI: { label: 'Chữ Hán', color: 'orange' },
  VOCABULARY: { label: 'Từ vựng', color: 'green' },
  GRAMMAR: { label: 'Ngữ pháp', color: 'magenta' },
}

interface Row {
  key: string
  ref: SaveLessonItemRef
  label: string
  sub?: string
  saved: boolean
}

const itemKey = (type: SectionType, id: number) => `${type}-${id}`
const groupKey = (sectionId: number) => `g-${sectionId}`

/** Gom mục của từng nhóm (giữ thứ tự bài) + đánh dấu mục đã có trong sổ tay. */
const buildGroups = (sections: LessonSection[], saved?: NotebookSavedIds) => {
  const savedVocab = new Set(saved?.vocabularyIds ?? [])
  const savedChar = new Set(saved?.characterIds ?? [])
  const savedGrammar = new Set(saved?.grammarIds ?? [])
  return sections
    .slice()
    .sort((a, b) => a.orderIndex - b.orderIndex)
    .map((s) => {
      const type = s.sectionType as SectionType
      const order = <T extends { orderIndex?: number }>(list?: T[]) =>
        (list ?? []).slice().sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
      let rows: Row[] = []
      if (type === 'KANJI') {
        rows = order(s.kanjiItems).map((k) => ({
          key: itemKey(type, k.id),
          ref: { type, itemId: k.id },
          label: k.character ?? '',
          sub: [k.hanViet, k.meaningVi].filter(Boolean).join(' · '),
          saved: k.characterId != null && savedChar.has(k.characterId),
        }))
      } else if (type === 'VOCABULARY') {
        rows = order(s.vocabularyItems).map((v) => ({
          key: itemKey(type, v.id),
          ref: { type, itemId: v.id },
          label: v.word ?? '',
          sub: [v.displayHiragana ?? v.hiragana, v.displayMeaning ?? v.meaningVi].filter(Boolean).join(' · '),
          saved: v.vocabId != null && savedVocab.has(v.vocabId),
        }))
      } else if (type === 'GRAMMAR') {
        rows = order(s.grammarItems).map((g) => ({
          key: itemKey(type, g.id),
          ref: { type, itemId: g.id },
          label: g.title ?? '',
          sub: g.displayMeaning ?? g.meaningVi,
          saved: g.grammarId != null && savedGrammar.has(g.grammarId),
        }))
      }
      return { section: s, type, rows }
    })
    .filter((g) => g.rows.length > 0)
}

/**
 * Chọn Chữ Hán / Từ vựng / Ngữ pháp của bài học để lưu vào sổ tay:
 * cả bài, theo nhóm (tick nhóm) hoặc từng mục. Bước 2 chọn sổ tay đích (sổ có sẵn hoặc tạo mới).
 */
export const SaveLessonToNotebookModal: React.FC<SaveLessonToNotebookModalProps> = ({
  open,
  onClose,
  lessonId,
  lessonTitle,
  sections,
  initialSectionId,
}) => {
  const { data: savedIds } = useNotebookSavedIds(open)
  const groups = useMemo(() => buildGroups(sections, savedIds), [sections, savedIds])
  const allRows = useMemo(() => groups.flatMap((g) => g.rows), [groups])
  const allKeys = useMemo(() => allRows.map((r) => r.key), [allRows])

  const [checked, setChecked] = useState<string[]>([])
  const [target, setTarget] = useState<LessonSaveTarget | null>(null)

  // Mỗi lần mở: chọn sẵn nhóm được truyền vào, không có thì cả bài
  useEffect(() => {
    if (!open) return
    const initial = groups.find((g) => g.section.id === initialSectionId)
    setChecked(initial ? initial.rows.map((r) => r.key) : allKeys)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initialSectionId])

  const treeData: TreeDataNode[] = groups.map((g) => ({
    key: groupKey(g.section.id),
    title: (
      <span className="font-semibold text-slate-700">
        <Tag color={SECTION_META[g.type].color} className="!mr-2">
          {SECTION_META[g.type].label}
        </Tag>
        {g.section.title}
        <span className="ml-2 text-xs font-normal text-slate-400">{g.rows.length} mục</span>
      </span>
    ),
    children: g.rows.map((r) => ({
      key: r.key,
      isLeaf: true,
      title: (
        <span>
          <span className="text-base text-slate-800">{r.label}</span>
          {r.sub && <span className="ml-2 text-xs text-slate-500">{r.sub}</span>}
          {r.saved && (
            <Tag color="blue" className="!ml-2">
              Đã lưu
            </Tag>
          )}
        </span>
      ),
    })),
  }))

  const selectedRows = allRows.filter((r) => checked.includes(r.key))
  const allChecked = allKeys.length > 0 && checked.length === allKeys.length
  const tooMany = selectedRows.length > MAX_ITEMS
  const counts = selectedRows.reduce(
    (acc, r) => ({ ...acc, [r.ref.type]: (acc[r.ref.type] ?? 0) + 1 }),
    {} as Partial<Record<SectionType, number>>,
  )
  const countText = (['VOCABULARY', 'KANJI', 'GRAMMAR'] as SectionType[])
    .filter((t) => counts[t])
    .map((t) => `${counts[t]} ${SECTION_META[t].label.toLowerCase()}`)
    .join(' · ')

  const handleNext = () => {
    if (selectedRows.length === 0) return
    // Chọn cả bài → gửi rỗng (BE lấy toàn bộ bài); ngược lại gửi danh sách mục
    const body = allChecked ? {} : { items: selectedRows.map((r) => r.ref) }
    setTarget({
      lessonId,
      ...body,
      label: allChecked ? `toàn bộ bài học (${countText})` : countText,
      suggestedName: lessonTitle,
    })
  }

  return (
    <>
      <Modal
        open={open && !target}
        onCancel={onClose}
        title="Lưu bài học vào sổ tay"
        width={680}
        destroyOnHidden
        footer={
          <div className="flex items-center justify-between gap-2">
            <span className={`text-sm ${tooMany ? 'text-red-500' : 'text-slate-500'}`}>
              Đã chọn {selectedRows.length}/{allKeys.length} mục
              {tooMany ? ` — tối đa ${MAX_ITEMS} mục mỗi lần lưu` : countText ? ` (${countText})` : ''}
            </span>
            <div className="flex gap-2">
              <Button onClick={onClose}>Huỷ</Button>
              <Button
                type="primary"
                icon={<BookmarkPlus className="h-4 w-4" />}
                disabled={selectedRows.length === 0 || tooMany}
                onClick={handleNext}
              >
                Chọn sổ tay
              </Button>
            </div>
          </div>
        }
      >
        {groups.length === 0 ? (
          <Empty description="Bài học chưa có chữ Hán, từ vựng hay ngữ pháp để lưu" />
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-slate-500">
              Tick tên nhóm để chọn cả nhóm, hoặc chọn từng mục. Một sổ tay chứa được cả 3 loại; mục đã có trong sổ
              được bỏ qua.
            </p>
            <Checkbox
              checked={allChecked}
              indeterminate={checked.length > 0 && !allChecked}
              onChange={(e) => setChecked(e.target.checked ? allKeys : [])}
            >
              <span className="font-medium">Chọn cả bài ({allKeys.length})</span>
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
                  setChecked(list.map(String).filter((k) => !k.startsWith('g-')))
                }}
              />
            </div>
          </div>
        )}
      </Modal>

      <AddWordToNotebookModal
        open={!!target}
        wordData={null}
        lessonSave={target}
        onClose={() => {
          setTarget(null)
          onClose()
        }}
      />
    </>
  )
}
