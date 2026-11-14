import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Input, Modal, Pagination, Select, Spin, Tag } from 'antd'
import { ArrowLeft, Download, Eye, Printer, Search, Crown } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { practiceSheetService, type MissingStrokeItem, type PracticeSheetType } from '@/services'
import type { ApiResponse, LessonSection } from '@/types'
import { toSlug } from '@/utils'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'

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
const PAGE_SIZES = [6, 10, 20]
const TYPE_LABEL: Record<PracticeSheetType, string> = { KANJI: 'chữ Hán', VOCABULARY: 'từ vựng' }

interface GroupItem {
  id: number
  label: string
  sub?: string
}

interface Preview {
  url: string
  blob: Blob
  missing: MissingStrokeItem[]
  total: number
}

const getErrorMessage = (err: unknown) =>
  (err as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? 'Lỗi không xác định'

/** Danh sách mục thiếu nét hiển thị trong bước xem trước. */
const MissingList: React.FC<{ items: MissingStrokeItem[]; total: number }> = ({ items, total }) => (
  <div className="space-y-2">
    <p className="text-sm text-slate-600">
      {items.length}/{total} mục có ký tự chưa có dữ liệu thứ tự nét. Các ký tự này vẫn được in chữ mẫu và chữ mờ để
      tô theo, nhưng <b>không có hàng thứ tự từng nét</b>.
    </p>
    <div className="max-h-32 overflow-y-auto rounded-md border border-slate-200 bg-white divide-y divide-slate-100">
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
  </div>
)

/** Trạng thái checkbox của một tập id so với danh sách đang chọn. */
const checkState = (ids: number[], selected: Set<number>) => {
  const n = ids.filter((id) => selected.has(id)).length
  return { checked: ids.length > 0 && n === ids.length, indeterminate: n > 0 && n < ids.length }
}

/**
 * Chọn mục Chữ Hán / Từ vựng của bài để in phiếu luyện viết (PDF):
 * tìm kiếm, chọn theo nhóm hoặc từng từ (phân trang trong mỗi nhóm), rồi xem trước PDF trước khi tải / in.
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
  const [query, setQuery] = useState('')
  const [pageSize, setPageSize] = useState(10)
  const [pages, setPages] = useState<Record<number, number>>({})
  const [busy, setBusy] = useState(false)
  const [preview, setPreview] = useState<Preview | null>(null)
  const iframeRef = useRef<HTMLIFrameElement>(null)

  // Mỗi lần mở: chọn sẵn nhóm được truyền vào, không có thì chọn tất cả
  useEffect(() => {
    if (!open) return
    const initial = groups.find((g) => g.section.id === initialSectionId)
    setChecked(initial ? initial.items.map((i) => i.id) : allIds)
    setQuery('')
    setPages({})
    setPreview(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initialSectionId])

  // Giải phóng object URL của bản xem trước cũ
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url) }, [preview])

  const selected = useMemo(() => new Set(checked), [checked])

  // Lọc theo từ khoá (chữ, Hán Việt, hiragana, nghĩa); ẩn nhóm không có kết quả
  const q = query.trim().toLowerCase()
  const visibleGroups = useMemo(
    () =>
      groups
        .map((g) => ({
          ...g,
          items: q ? g.items.filter((i) => `${i.label} ${i.sub ?? ''}`.toLowerCase().includes(q)) : g.items,
        }))
        .filter((g) => g.items.length > 0),
    [groups, q],
  )
  const visibleIds = visibleGroups.flatMap((g) => g.items.map((i) => i.id))

  /** Thêm / bỏ một tập id khỏi danh sách chọn (giữ nguyên các id khác, kể cả ở trang/nhóm khác). */
  const toggle = (ids: number[], on: boolean) =>
    setChecked((prev) => {
      const next = new Set(prev)
      ids.forEach((id) => (on ? next.add(id) : next.delete(id)))
      return allIds.filter((id) => next.has(id))
    })

  const user = useAuthStore((s) => s.user)
  const isProStore = useSubscriptionStore((s) => s.isPro)
  const openPaywall = useSubscriptionStore((s) => s.openPaywall)
  const isUserPro = isProStore || !!user?.isPro

  const label = TYPE_LABEL[type]
  const tooMany = checked.length > MAX_ITEMS
  const tooManyForFree = !isUserPro && checked.length > 10

  const handleClose = () => {
    setPreview(null)
    onClose()
  }

  const handlePreview = async () => {
    if (tooManyForFree) {
      openPaywall('Tài khoản Miễn phí được xuất tối đa 10 từ/phiếu PDF. Hãy nâng cấp PRO để xuất không giới hạn toàn bộ bài học!')
      return
    }

    // Giữ thứ tự theo bài học (BE cũng sắp lại theo nhóm → thứ tự trong nhóm)
    const itemIds = allIds.filter((id) => selected.has(id))
    if (itemIds.length === 0) return
    const body = { sectionType: type, itemIds }
    setBusy(true)
    try {
      const [res, blob] = await Promise.all([
        practiceSheetService.check(lessonId, body),
        practiceSheetService.generate(lessonId, body),
      ])
      setPreview({
        url: URL.createObjectURL(blob),
        blob,
        missing: res.data?.missingItems ?? [],
        total: res.data?.totalItems ?? itemIds.length,
      })
    } catch (e) {
      toast.error('Tạo phiếu luyện viết thất bại', { description: getErrorMessage(e) })
    } finally {
      setBusy(false)
    }
  }

  const handleDownload = () => {
    if (!preview) return
    const slug = toSlug(lessonTitle ?? '') || `bai-${lessonId}`
    const prefix = type === 'KANJI' ? 'luyen_viet_chu_han' : 'luyen_viet_tu_vung'
    practiceSheetService.saveFile(preview.blob, `${prefix}_${slug}.pdf`)
    toast.success('Đã tải phiếu luyện viết')
  }

  const handlePrint = () => {
    if (!preview) return
    try {
      iframeRef.current?.contentWindow?.print()
    } catch {
      // Trình duyệt chặn in trong iframe (vd Safari di động) → mở PDF ở tab mới để in
      window.open(preview.url, '_blank')
    }
  }

  const footer = preview ? (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <Button icon={<ArrowLeft className="h-4 w-4" />} onClick={() => setPreview(null)}>
        Chọn lại
      </Button>
      <div className="flex gap-2">
        <Button icon={<Download className="h-4 w-4" />} onClick={handleDownload}>
          Tải PDF
        </Button>
        <Button type="primary" icon={<Printer className="h-4 w-4" />} onClick={handlePrint}>
          In
        </Button>
      </div>
    </div>
  ) : (
    <div className="flex items-center justify-between gap-2">
      <div className="text-xs">
        <span className={tooMany || tooManyForFree ? 'text-amber-600 font-medium' : 'text-slate-500'}>
          Đã chọn {checked.length}/{allIds.length} mục
          {tooMany ? ` — tối đa ${MAX_ITEMS} mục mỗi lần in` : ''}
          {tooManyForFree && (
            <span className="ml-1 text-rose-500 font-semibold">
              (Miễn phí tối đa 10 chữ/phiếu)
            </span>
          )}
        </span>
        {tooManyForFree && (
          <button
            type="button"
            onClick={() => openPaywall('Phiếu luyện viết chữ Hán trên 10 từ yêu cầu gói PRO')}
            className="ml-2 text-amber-600 hover:text-amber-700 font-bold underline inline-flex items-center gap-0.5 cursor-pointer"
          >
            <Crown className="w-3 h-3" />
            Nâng cấp PRO
          </button>
        )}
      </div>
      <div className="flex gap-2">
        <Button onClick={handleClose}>Huỷ</Button>
        <Button
          type="primary"
          icon={<Eye className="h-4 w-4" />}
          loading={busy}
          disabled={checked.length === 0 || tooMany || tooManyForFree}
          onClick={handlePreview}
        >
          Xem trước
        </Button>
      </div>
    </div>
  )

  return (
    <Modal
      open={open}
      onCancel={handleClose}
      title={`In phiếu luyện viết ${label}`}
      width={preview ? 960 : 640}
      destroyOnHidden
      footer={footer}
    >
      {preview ? (
        <div className="space-y-3">
          {preview.missing.length > 0 && (
            <Alert
              type="warning"
              showIcon
              title="Một số mục thiếu dữ liệu thứ tự nét"
              description={<MissingList items={preview.missing} total={preview.total} />}
            />
          )}
          <iframe
            ref={iframeRef}
            src={preview.url}
            title="Xem trước phiếu luyện viết"
            className="h-[70vh] w-full rounded-md border border-slate-200"
          />
        </div>
      ) : groups.length === 0 ? (
        <Empty description={`Bài học chưa có ${label} để in`} />
      ) : (
        <Spin spinning={busy} description="Đang tạo bản xem trước...">
          <div className="space-y-3">
            <p className="text-sm text-slate-500">
              Tick tên nhóm để chọn cả nhóm, hoặc chọn từng {label}. Mỗi nhóm được in thành một phần riêng trong file
              PDF.
            </p>
            <div className="flex flex-wrap items-stretch gap-2">
              <Input
                allowClear
                prefix={<Search className="h-4 w-4 text-slate-400" aria-hidden />}
                placeholder={`Tìm ${label} (chữ, cách đọc, nghĩa)`}
                className="min-w-0 flex-1"
                onChange={(e) => {
                  setQuery(e.target.value)
                  setPages({})
                }}
              />
              <span className="flex items-center gap-1.5 text-sm text-slate-500">
                Mỗi trang
                <Select
                  aria-label="Số mục mỗi trang"
                  value={pageSize}
                  onChange={(v) => {
                    setPageSize(v)
                    setPages({})
                  }}
                  options={PAGE_SIZES.map((n) => ({ value: n, label: n }))}
                  className="w-20 self-stretch !h-auto"
                />
              </span>
            </div>
            {/* Chỉ hiện "Chọn tất cả" khi có ≥ 2 nhóm — 1 nhóm thì checkbox nhóm đã làm việc này */}
            {visibleGroups.length >= 2 && (
              <Checkbox
                {...checkState(visibleIds, selected)}
                onChange={(e) => toggle(visibleIds, e.target.checked)}
              >
                <span className="font-medium">
                  {q ? `Chọn tất cả kết quả (${visibleIds.length})` : `Chọn tất cả (${allIds.length})`}
                </span>
              </Checkbox>
            )}
            <div className="max-h-[50vh] space-y-3 overflow-y-auto">
              {visibleGroups.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`Không tìm thấy ${label} phù hợp`} />
              ) : (
                visibleGroups.map((g) => {
                  const ids = g.items.map((i) => i.id)
                  const page = Math.min(pages[g.section.id] ?? 1, Math.ceil(g.items.length / pageSize))
                  const pageItems = g.items.slice((page - 1) * pageSize, page * pageSize)
                  return (
                    <div key={g.section.id} className="rounded-md border border-slate-200">
                      <div className="border-b border-slate-100 bg-slate-50 px-3 py-2">
                        <Checkbox {...checkState(ids, selected)} onChange={(e) => toggle(ids, e.target.checked)}>
                          <span className="font-semibold text-slate-700">
                            {g.section.title}
                            <span className="ml-2 text-xs font-normal text-slate-400">{g.items.length} mục</span>
                          </span>
                        </Checkbox>
                      </div>
                      <div className="divide-y divide-slate-50 px-3">
                        {pageItems.map((i) => (
                          <Checkbox
                            key={i.id}
                            className="!flex py-1.5"
                            checked={selected.has(i.id)}
                            onChange={(e) => toggle([i.id], e.target.checked)}
                          >
                            <span className="text-base text-slate-800">{i.label}</span>
                            {i.sub && <span className="ml-2 text-xs text-slate-500">{i.sub}</span>}
                          </Checkbox>
                        ))}
                      </div>
                      {g.items.length > pageSize && (
                        <div className="flex justify-end border-t border-slate-100 px-3 py-1.5">
                          <Pagination
                            size="small"
                            current={page}
                            pageSize={pageSize}
                            total={g.items.length}
                            showSizeChanger={false}
                            onChange={(p) => setPages((prev) => ({ ...prev, [g.section.id]: p }))}
                          />
                        </div>
                      )}
                    </div>
                  )
                })
              )}
            </div>
          </div>
        </Spin>
      )}
    </Modal>
  )
}
