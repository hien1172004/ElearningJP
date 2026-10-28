import React from 'react'
import { Modal } from 'antd'
import type { ModalFuncProps } from 'antd'

interface ConfirmOptions {
  title: string
  content?: React.ReactNode
  okText?: string
  cancelText?: string
  okType?: 'primary' | 'danger' | 'default'
  danger?: boolean
}

/**
 * Helper gọi `Modal.confirm` của antd với promise wrapper.
 * Trả về Promise<boolean> - true nếu user xác nhận, false nếu huỷ.
 */
export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    const props: ModalFuncProps = {
      title: options.title,
      content: options.content,
      okText: options.okText ?? 'Xác nhận',
      cancelText: options.cancelText ?? 'Huỷ',
      okButtonProps: { danger: options.danger ?? options.okType === 'danger' },
      centered: true,
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
      title={title}
      onOk={onConfirm}
      onCancel={onCancel}
      okText={okText}
      cancelText={cancelText}
      okButtonProps={{ danger, loading: isLoading }}
      confirmLoading={isLoading}
      centered
      maskClosable={!isLoading}
    >
      {content}
    </Modal>
  )
}
