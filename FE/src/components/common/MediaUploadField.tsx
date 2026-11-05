import React from 'react'
import { X as XIcon } from 'lucide-react'
import { FileUploadButton } from '@/components/exam/FileUploadButton'

interface MediaUploadFieldProps {
  /** Giá trị URL hiện tại (antd Form.Item tự truyền vào) */
  value?: string
  /** Callback cập nhật URL (antd Form.Item tự truyền vào) */
  onChange?: (value: string) => void
  /** Loại media cho phép tải lên */
  accept: 'image' | 'audio'
  /** Thư mục lưu trên Cloudinary */
  folder: string
  disabled?: boolean
}

/**
 * Trường form tải ảnh/audio lên Cloudinary rồi lưu URL trả về.
 * Dùng trực tiếp trong antd `Form.Item` thay cho ô dán link.
 */
export const MediaUploadField: React.FC<MediaUploadFieldProps> = ({
  value,
  onChange,
  accept,
  folder,
  disabled = false,
}) => {
  const isAudio = accept === 'audio'

  return (
    <div className="flex flex-col gap-2">
      {value && (
        <div className="flex items-center gap-2">
          {isAudio ? (
            <audio src={value} controls className="h-9 max-w-full flex-1" />
          ) : (
            <img
              src={value}
              alt="Ảnh đã tải lên"
              className="max-h-28 rounded-lg border border-gray-200 object-contain"
            />
          )}
          <button
            type="button"
            disabled={disabled}
            onClick={() => onChange?.('')}
            className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-rose-600 hover:bg-rose-50 disabled:opacity-50"
          >
            <XIcon className="h-3.5 w-3.5" /> Gỡ
          </button>
        </div>
      )}
      <div>
        <FileUploadButton
          accept={accept}
          folder={folder}
          disabled={disabled}
          buttonText={value ? (isAudio ? 'Đổi audio' : 'Đổi ảnh') : undefined}
          onUploadSuccess={(url) => onChange?.(url)}
        />
      </div>
    </div>
  )
}
