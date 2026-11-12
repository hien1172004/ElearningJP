import React, { useState } from 'react'
import { Input, Modal, Radio } from 'antd'
import { isAxiosError } from 'axios'
import { RequiredMark } from '@/components/common'
import { useReportComment } from '@/hooks/useReportComment'
import { REPORT_REASON_LABELS } from './reportReasons'
import type { ReportReason } from '@/types'

const REASONS = Object.keys(REPORT_REASON_LABELS) as ReportReason[]
const MAX_DETAIL_LENGTH = 500

interface ReportCommentModalProps {
  open: boolean
  kind: 'comment' | 'reply'
  targetId: number
  onClose: () => void
}

/** Modal báo cáo vi phạm một bình luận hoặc trả lời. */
export const ReportCommentModal: React.FC<ReportCommentModalProps> = ({
  open,
  kind,
  targetId,
  onClose,
}) => {
  const [reason, setReason] = useState<ReportReason | null>(null)
  const [detail, setDetail] = useState('')
  const report = useReportComment()

  const handleClose = () => {
    setReason(null)
    setDetail('')
    onClose()
  }

  const handleSubmit = () => {
    if (!reason) return
    report.mutate(
      { kind, id: targetId, payload: { reason, detail: detail.trim() || undefined } },
      {
        onSuccess: handleClose,
        // Đã báo cáo rồi (409) cũng đóng modal: người dùng không cần làm gì thêm
        onError: (error) => {
          if (isAxiosError(error) && error.response?.status === 409) handleClose()
        },
      },
    )
  }

  return (
    <Modal
      open={open}
      title={kind === 'reply' ? 'Báo cáo trả lời' : 'Báo cáo bình luận'}
      okText="Gửi báo cáo"
      cancelText="Huỷ"
      okButtonProps={{ danger: true, disabled: !reason }}
      confirmLoading={report.isPending}
      onOk={handleSubmit}
      onCancel={handleClose}
      centered
      destroyOnHidden
    >
      <p className="text-sm text-gray-600 mb-3">
        Nội dung này vi phạm quy định cộng đồng như thế nào?
        <RequiredMark />
      </p>
      <Radio.Group
        value={reason}
        onChange={(e) => setReason(e.target.value as ReportReason)}
        className="!flex !flex-col gap-2"
      >
        {REASONS.map((r) => (
          <Radio key={r} value={r}>
            {REPORT_REASON_LABELS[r]}
          </Radio>
        ))}
      </Radio.Group>
      <Input.TextArea
        value={detail}
        onChange={(e) => setDetail(e.target.value)}
        placeholder="Mô tả thêm"
        maxLength={MAX_DETAIL_LENGTH}
        showCount
        autoSize={{ minRows: 2, maxRows: 5 }}
        className="!mt-4"
      />
    </Modal>
  )
}
