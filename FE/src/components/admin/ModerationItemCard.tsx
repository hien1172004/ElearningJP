import React from 'react'
import { Avatar, Button as AntdButton, Tag, Tooltip } from 'antd'
import { Eye, EyeOff, ExternalLink, Flag, ShieldCheck, Trash2 } from 'lucide-react'
import { REPORT_REASON_LABELS } from '@/components/comment'
import { formatRelativeTime } from '@/utils/format'
import { TARGET_TYPE_LABELS } from './moderationLabels'
import type { ModerationAction, ModerationItem } from '@/types'

const STATUS_META: Record<ModerationItem['status'], { label: string; color: string }> = {
  VISIBLE: { label: 'Đang hiển thị', color: 'green' },
  HIDDEN: { label: 'Đã ẩn', color: 'orange' },
  DELETED: { label: 'Đã xóa', color: 'red' },
}

interface ModerationItemCardProps {
  item: ModerationItem
  /** Hiện nút "Bỏ qua báo cáo" (chỉ ở hàng chờ báo cáo đang mở). */
  showDismiss?: boolean
  /** Thao tác đang chạy cho đúng mục này → khoá các nút. */
  busy?: boolean
  onAction: (item: ModerationItem, action: ModerationAction) => void
  /** Mở bình luận tại trang gốc (chỉ khi đang hiển thị). */
  onOpen?: (item: ModerationItem) => void
}

/** Một bình luận / trả lời trên trang kiểm duyệt, kèm lý do báo cáo và các thao tác. */
export const ModerationItemCard: React.FC<ModerationItemCardProps> = ({
  item,
  showDismiss = false,
  busy = false,
  onAction,
  onOpen,
}) => {
  const status = STATUS_META[item.status]
  const where = [TARGET_TYPE_LABELS[item.targetType], item.targetLabel].filter(Boolean).join(': ')

  return (
    <article className="bg-white rounded-xl border border-slate-200 p-4 space-y-3">
      {/* Người viết + trạng thái */}
      <div className="flex items-start gap-3">
        <Avatar src={item.author?.avatarUrl || undefined} className="shrink-0 bg-indigo-100 text-indigo-700">
          {item.author?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
        </Avatar>
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-medium text-slate-800 truncate">
              {item.author?.fullName || 'Người dùng đã xóa'}
            </span>
            {item.author?.email && <span className="text-xs text-slate-500 truncate">{item.author.email}</span>}
            <span className="text-xs text-slate-400">· {formatRelativeTime(item.createdAt)}</span>
          </div>
          <div className="mt-1 flex flex-wrap items-center gap-1.5">
            <Tag className="rounded-md !m-0">{item.kind === 'REPLY' ? 'Trả lời' : 'Bình luận'}</Tag>
            <Tag color={status.color} className="rounded-md !m-0">
              {status.label}
            </Tag>
            <span className="text-xs text-slate-500 truncate">
              {where}
              {item.courseTitle ? ` · ${item.courseTitle}` : ''}
            </span>
          </div>
        </div>
      </div>

      {/* Nội dung */}
      <p className="text-sm text-slate-800 whitespace-pre-wrap break-words bg-slate-50 rounded-lg px-3 py-2 line-clamp-6">
        {item.content}
      </p>

      {/* Báo cáo */}
      {item.reportCount > 0 && (
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-1.5 text-xs">
            <span className="inline-flex items-center gap-1 font-semibold text-amber-700">
              <Flag className="w-3.5 h-3.5" />
              {item.reportCount} báo cáo
            </span>
            {item.reasons.map((r) => (
              <Tag key={r.reason} className="rounded-md !m-0">
                {REPORT_REASON_LABELS[r.reason]} ({r.count})
              </Tag>
            ))}
            {item.latestReportAt && (
              <span className="text-slate-400">· gần nhất {formatRelativeTime(item.latestReportAt)}</span>
            )}
          </div>
          {item.recentDetails.length > 0 && (
            <ul className="text-xs text-slate-600 space-y-0.5 pl-5 list-disc">
              {item.recentDetails.map((d, i) => (
                <li key={i} className="break-words">
                  {d}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Thao tác */}
      {item.status !== 'DELETED' && (
        <div className="flex flex-wrap justify-end gap-2 pt-1">
          {item.status === 'VISIBLE' && onOpen && (
            <Tooltip title="Mở bình luận tại trang gốc">
              <AntdButton size="small" icon={<ExternalLink className="w-4 h-4" />} onClick={() => onOpen(item)}>
                Xem
              </AntdButton>
            </Tooltip>
          )}
          {showDismiss && (
            <AntdButton
              size="small"
              icon={<ShieldCheck className="w-4 h-4" />}
              disabled={busy}
              onClick={() => onAction(item, 'dismiss')}
            >
              Không vi phạm
            </AntdButton>
          )}
          {item.status === 'VISIBLE' ? (
            <AntdButton
              size="small"
              icon={<EyeOff className="w-4 h-4" />}
              disabled={busy}
              onClick={() => onAction(item, 'hide')}
            >
              Ẩn
            </AntdButton>
          ) : (
            <AntdButton
              size="small"
              icon={<Eye className="w-4 h-4" />}
              disabled={busy}
              onClick={() => onAction(item, 'restore')}
            >
              Khôi phục
            </AntdButton>
          )}
          <AntdButton
            size="small"
            danger
            icon={<Trash2 className="w-4 h-4" />}
            disabled={busy}
            onClick={() => onAction(item, 'delete')}
          >
            Xóa
          </AntdButton>
        </div>
      )}
    </article>
  )
}
