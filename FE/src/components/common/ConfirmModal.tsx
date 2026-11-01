import React from 'react'
import { Modal } from 'antd'
import type { ModalFuncProps } from 'antd'

interface ConfirmOptions {
  title: string
  content?: React.ReactNode
  okText?: string
  cancelText?: string
  danger?: boolean
}

/**
 * Helper gọi `Modal.confirm` của antd với promise wrapper.
 * Trả về Promise<boolean> - true nếu user xác nhận, false nếu huỷ.
 */
export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    const isDanger = options.danger ?? false
    const props: ModalFuncProps = {
      title: (
        <span className="font-bold text-slate-800 text-base">
          {options.title}
        </span>
      ),
      content:
        typeof options.content === 'string' ? (
          <div className="text-sm text-slate-600 whitespace-pre-line leading-relaxed mt-2.5">
            {options.content}
          </div>
        ) : (
          options.content
        ),
      okText: options.okText ?? 'Xác nhận',
      cancelText: options.cancelText ?? 'Huỷ',
      okButtonProps: {
        danger: isDanger,
        className: '!rounded-lg font-medium shadow-xs',
      },
      cancelButtonProps: {
        className: '!rounded-lg font-medium text-slate-600 hover:text-slate-800',
      },
      centered: true,
      maskClosable: true,
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    }
    Modal.confirm(props)
  })
}

interface ConfirmModalProps {
  open: boolean
  title: string
  content?: React.ReactNode
  okText?: string
  cancelText?: string
  danger?: boolean
  isLoading?: boolean
  onConfirm: () => void | Promise<void>
  onCancel: () => void
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  open,
  title,
  content,
  okText = 'Xác nhận',
  cancelText = 'Huỷ',
  danger = false,
  isLoading = false,
  onConfirm,
  onCancel,
}) => {
  return (
    <Modal
      open={open}
      title={<span className="font-bold text-slate-800">{title}</span>}
      onOk={onConfirm}
      onCancel={onCancel}
      okText={okText}
      cancelText={cancelText}
      okButtonProps={{ danger, loading: isLoading, className: '!rounded-lg font-medium shadow-xs' }}
      cancelButtonProps={{ className: '!rounded-lg' }}
      confirmLoading={isLoading}
      centered
      maskClosable={!isLoading}
    >
      {typeof content === 'string' ? (
        <div className="text-sm text-slate-600 whitespace-pre-line leading-relaxed mt-2.5">
          {content}
        </div>
      ) : (
        content
      )}
    </Modal>
  )
}
