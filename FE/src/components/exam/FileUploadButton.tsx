import React, { useRef, useState } from 'react'
import { Loader2, Image as ImageIcon, Music } from 'lucide-react'
import { mediaService } from '@/services/media.service'

interface FileUploadButtonProps {
  onUploaded?: (url: string) => void
  onUploadSuccess?: (url: string) => void
  accept?: 'image' | 'audio' | string
  label?: string
  buttonText?: string
  disabled?: boolean
  className?: string
  folder?: string
}

export const FileUploadButton: React.FC<FileUploadButtonProps> = ({
  onUploaded,
  onUploadSuccess,
  accept = 'image',
  label,
  buttonText,
  disabled = false,
  className = '',
  folder = 'elearningJP/exam',
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isAudio = accept === 'audio' || (typeof accept === 'string' && accept.includes('audio'))
  const acceptMime = isAudio
    ? 'audio/mp3,audio/mpeg,audio/wav,audio/m4a'
    : 'image/png,image/jpeg,image/webp,image/gif'
  const defaultLabel = isAudio ? 'Tải audio từ máy' : 'Tải ảnh từ máy'
  const displayLabel = buttonText || label || defaultLabel

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    try {
      setUploading(true)
      setError(null)
      const uploadedUrl = await mediaService.uploadFile(file, folder)
      const callback = onUploadSuccess || onUploaded
      if (callback) {
        callback(uploadedUrl)
      }
    } catch (err: any) {
      console.error('Lỗi khi tải file lên máy chủ:', err)
      setError(err?.response?.data?.message || 'Không thể tải file lên. Vui lòng thử lại.')
    } finally {
      setUploading(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  return (
    <div className="inline-flex flex-col">
      <input
        ref={fileInputRef}
        type="file"
        accept={acceptMime}
        onChange={handleFileChange}
        disabled={disabled || uploading}
        className="hidden"
      />

      <button
        type="button"
        disabled={disabled || uploading}
        onClick={() => fileInputRef.current?.click()}
        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
          uploading
            ? 'bg-indigo-100 text-indigo-400 cursor-not-allowed'
            : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-200 shadow-2xs'
        } ${className}`}
      >
        {uploading ? (
          <>
            <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-600" />
            <span>Đang tải lên...</span>
          </>
        ) : (
          <>
            {isAudio ? (
              <Music className="w-3.5 h-3.5 text-indigo-600" />
            ) : (
              <ImageIcon className="w-3.5 h-3.5 text-indigo-600" />
            )}
            <span>{displayLabel}</span>
          </>
        )}
      </button>

      {error && <span className="text-[10px] text-rose-500 font-medium mt-1">{error}</span>}
    </div>
  )
}
