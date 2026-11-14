import React, { useState } from 'react'
import { Button as AntdButton, Modal, Tag, Tooltip } from 'antd'
import { AlertTriangle, History, RefreshCw, Trash2 } from 'lucide-react'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { EmptyState, LoadingState } from '@/components/common'
import { useArchivedLessonDraft, useLessonDraftActions, useLessonReviewRequestContent } from '@/hooks'
import type { useCourseEditSession } from '@/hooks'
import type { LessonDraftSnapshot, LessonReviewState, LessonSection } from '@/types'

interface LessonDraftPanelProps {
  lessonId: number
  state?: LessonReviewState | null
  admin: boolean
  /**
   * Phiên sửa khóa học của trang ({@code useCourseEditSession}) — bỏ nháp / cập nhật bản mới cần giữ khóa sửa phía mình.
   * "Cập nhật bản mới" khi chưa giữ khóa → tự bắt đầu sửa rồi cập nhật luôn.
   */
  session: ReturnType<typeof useCourseEditSession>
  /** Lý do không bắt đầu sửa được (giống nút "Sửa khóa học") — chặn tự bắt đầu sửa khi cập nhật bản mới. */
  startBlockedReason?: string | null
}

const SECTION_LABEL: Record<string, string> = { KANJI: 'Chữ Hán', VOCABULARY: 'Từ vựng', GRAMMAR: 'Ngữ pháp' }

/**
 * Nháp của phía mình (V65 — giáo viên và quản trị viên sửa 2 bản nháp riêng):
 * - Nháp đã cũ (phía kia vừa đưa bài lên bản học viên) → báo + "Cập nhật bản mới" (nháp hiện tại giữ lại chỉ đọc).
 * - "Bỏ nháp": nháp quay về đúng bản học viên đang học.
 * - "Xem nháp cũ": nội dung nháp được giữ lại ở lần cập nhật bản mới gần nhất, để chép lại phần còn thiếu.
 */
export const LessonDraftPanel: React.FC<LessonDraftPanelProps> = ({ lessonId, state, admin, session, startBlockedReason }) => {
  const { discard, rebase } = useLessonDraftActions(lessonId)
  const [archiveOpen, setArchiveOpen] = useState(false)
  if (!state) return null

  const editing = session.mine
  // Nháp giáo viên đã gửi duyệt → chỉ đọc (nháp ADMIN không bị ảnh hưởng)
  const submitted = !admin && state.reviewStatus === 'PENDING_REVIEW'
  const submittedReason = 'Bài đã gửi duyệt — rút yêu cầu hoặc chờ quản trị viên quyết định trước'
  const blocked = submitted
    ? submittedReason
    : !editing
      ? 'Bấm "Sửa khóa học" để bắt đầu sửa trước'
      : undefined
  // Cập nhật bản mới: chưa giữ khóa thì tự bắt đầu sửa — chỉ chặn khi người cùng phía đang sửa / không bắt đầu sửa được
  const rebaseBlocked = submitted
    ? submittedReason
    : editing
      ? undefined
      : session.heldByOther
        ? `${session.lock?.userName ?? 'Người khác'} đang sửa khóa học — chờ họ bấm "Xong" hoặc tiếp quản trước`
        : startBlockedReason ?? undefined
  const canDiscard = state.hasUnpublishedChanges && state.inPublishedVersion
  if (!state.outdated && !canDiscard && !state.hasArchivedDraft) return null

  const handleRebase = async () => {
    const ok = await confirmDialog({
      title: 'Cập nhật bản mới?',
      content:
        'Nháp hiện tại được giữ lại ở chế độ chỉ đọc ("Xem nháp cũ"); nháp mới lấy đúng bản học viên đang học. ' +
        'Bạn tự chép lại phần còn thiếu từ nháp cũ. Nháp cũ trước đó (nếu có) sẽ bị thay.' +
        (editing ? '' : ' Thao tác này sẽ bắt đầu phiên sửa khóa học của bạn (bấm "Xong" khi sửa xong).'),
      okText: 'Cập nhật bản mới',
      cancelText: 'Hủy',
    })
    if (!ok) return
    if (!session.mine) {
      try {
        await session.start.mutateAsync(false)
      } catch {
        return // start đã báo lỗi (vd: người khác vừa bắt đầu sửa)
      }
    }
    rebase.mutate()
  }

  const handleDiscard = async () => {
    const ok = await confirmDialog({
      title: 'Bỏ nháp của bài này?',
      content: `Mọi thay đổi chưa đưa lên bản học viên của ${admin ? 'quản trị viên' : 'giáo viên'} trong bài bị xóa, `
        + 'bài quay về đúng bản học viên đang học. Không khôi phục được.',
      okText: 'Bỏ nháp',
      cancelText: 'Hủy',
      danger: true,
    })
    if (ok) discard.mutate()
  }

  return (
    <div className="space-y-2">
      {state.outdated && (
        <div role="alert" className="rounded-lg border border-orange-200 bg-orange-50/70 px-3 py-2 text-sm text-orange-800 flex flex-wrap items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <p className="flex-1 min-w-60">
            <strong>Bản học viên đang học đã thay đổi</strong> sau khi bản nháp này được tạo
            ({admin ? 'giáo viên vừa được duyệt bài' : 'quản trị viên vừa lưu bài'}). Cần cập nhật bản mới trước khi{' '}
            {admin ? 'lưu bài' : 'gửi duyệt'}.
          </p>
          <Tooltip title={rebaseBlocked}>
            <AntdButton
              size="small"
              type="primary"
              icon={<RefreshCw className="w-3.5 h-3.5" />}
              disabled={!!rebaseBlocked}
              loading={rebase.isPending || session.start.isPending}
              onClick={handleRebase}
            >
              Cập nhật bản mới
            </AntdButton>
          </Tooltip>
        </div>
      )}
      {(canDiscard || state.hasArchivedDraft) && (
        <div className="flex flex-wrap items-center justify-end gap-2">
          {state.hasArchivedDraft && (
            <AntdButton size="small" icon={<History className="w-3.5 h-3.5" />} onClick={() => setArchiveOpen(true)}>
              Xem nháp cũ
            </AntdButton>
          )}
          {canDiscard && (
            <Tooltip title={blocked}>
              <AntdButton
                size="small"
                danger
                icon={<Trash2 className="w-3.5 h-3.5" />}
                disabled={!!blocked}
                loading={discard.isPending}
                onClick={handleDiscard}
              >
                Bỏ nháp
              </AntdButton>
            </Tooltip>
          )}
        </div>
      )}
      <ArchivedDraftModal lessonId={lessonId} open={archiveOpen} onClose={() => setArchiveOpen(false)} />
    </div>
  )
}

/** Nội dung nháp cũ chỉ đọc — liệt kê theo nhóm để chép lại. */
const ArchivedDraftModal: React.FC<{ lessonId: number; open: boolean; onClose: () => void }> = ({ lessonId, open, onClose }) => {
  const { data, isLoading, isError } = useArchivedLessonDraft(lessonId, open)
  return (
    <Modal open={open} onCancel={onClose} footer={null} width={720} title="Nháp cũ (chỉ đọc)" destroyOnHidden>
      {isLoading ? (
        <LoadingState message="Đang tải nháp cũ..." />
      ) : isError || !data ? (
        <EmptyState title="Không tải được nháp cũ" />
      ) : (
        <LessonSnapshotContent
          draft={data}
          hint="Mở song song trang soạn bài để chép lại phần cần giữ. Nháp cũ không sửa được và bị thay ở lần cập nhật bản mới kế tiếp."
        />
      )}
    </Modal>
  )
}

/**
 * ADMIN xem bản giáo viên đã gửi duyệt (chỉ đọc) — đúng nội dung sẽ lên bản học viên khi duyệt. ADMIN không đọc được
 * nháp đang soạn của giáo viên (V65/V66).
 */
export const SubmittedLessonModal: React.FC<{
  requestId?: number | null
  title?: string
  open: boolean
  onClose: () => void
}> = ({ requestId, title, open, onClose }) => {
  const { data, isLoading, isError } = useLessonReviewRequestContent(requestId, open)
  return (
    <Modal open={open} onCancel={onClose} footer={null} width={720} destroyOnHidden
      title={title ? `Bản giáo viên gửi duyệt — ${title}` : 'Bản giáo viên gửi duyệt'}>
      {isLoading ? (
        <LoadingState message="Đang tải bản đã gửi..." />
      ) : isError || !data ? (
        <EmptyState title="Không tải được bản đã gửi" />
      ) : (
        <LessonSnapshotContent draft={data} hint="Duyệt sẽ đưa đúng nội dung này lên bản học viên đang học." />
      )}
    </Modal>
  )
}

const LessonSnapshotContent: React.FC<{ draft: LessonDraftSnapshot; hint: string }> = ({ draft, hint }) => {
  const sections = draft.sections ?? []
  const questions = draft.quiz?.questions ?? []
  const speaking = draft.speaking ?? []
  if (sections.length === 0 && questions.length === 0 && speaking.length === 0) {
    return <EmptyState title="Không có nội dung" />
  }
  return (
    <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-1">
      <p className="text-xs text-gray-500">{hint}</p>
      {sections.map((s, i) => (
        <SectionBlock key={s.id ?? i} section={s} />
      ))}
      {questions.length > 0 && (
        <div className="space-y-1">
          <p className="text-sm font-semibold text-gray-800">Quiz{draft.quiz?.title ? ` — ${draft.quiz.title}` : ''}</p>
          <ol className="list-decimal pl-5 text-sm text-gray-700 space-y-0.5">
            {questions.map((q, i) => (
              <li key={q.id ?? i}>{q.questionText}</li>
            ))}
          </ol>
        </div>
      )}
      {speaking.length > 0 && (
        <div className="space-y-1">
          <p className="text-sm font-semibold text-gray-800">Hội thoại</p>
          <ul className="list-disc pl-5 text-sm text-gray-700">
            {speaking.map((sp, i) => (
              <li key={sp.scenarioId ?? i}>{sp.title || `Kịch bản #${sp.scenarioId}`}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

const SectionBlock: React.FC<{ section: LessonSection }> = ({ section }) => {
  const rows: string[] = [
    ...(section.kanjiItems ?? []).map((k) => [k.character, k.meaningVi].filter(Boolean).join(' — ')),
    ...(section.vocabularyItems ?? []).map((v) => [v.word, v.meaningVi].filter(Boolean).join(' — ')),
    ...(section.grammarItems ?? []).map((g) =>
      [g.title, g.displayMeaning ?? g.meaningVi].filter(Boolean).join(' — '),
    ),
  ]
  return (
    <div className="space-y-1">
      <p className="text-sm font-semibold text-gray-800 flex items-center gap-2">
        <Tag className="rounded-md !m-0">{SECTION_LABEL[section.sectionType] ?? section.sectionType}</Tag>
        {section.title}
      </p>
      {rows.length === 0 ? (
        <p className="text-xs text-gray-400">Chưa có mục nào</p>
      ) : (
        <ul className="list-disc pl-5 text-sm text-gray-700 space-y-0.5">
          {rows.map((r, i) => (
            <li key={i}>{r}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
