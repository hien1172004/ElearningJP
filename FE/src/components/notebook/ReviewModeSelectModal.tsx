import React, { useState, useEffect } from 'react'
import { Modal, Switch, Select } from 'antd'
import { useNavigate } from 'react-router-dom'
import {
  SoundOutlined,
  ArrowLeftOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import type { Notebook } from '@/types'

interface ReviewModeSelectModalProps {
  open: boolean
  onClose: () => void
  notebooks: Notebook[]
  defaultNotebookId?: number
  srsEnabled?: boolean
  onToggleSrs?: (enabled: boolean) => void
}

type ModalStep = 'select-mode' | 'quiz-options'

export const ReviewModeSelectModal: React.FC<ReviewModeSelectModalProps> = ({
  open,
  onClose,
  notebooks,
  defaultNotebookId,
  srsEnabled = true,
  onToggleSrs,
}) => {
  const navigate = useNavigate()

  const totalWords = notebooks.reduce((acc, nb) => acc + (nb.wordCount || 0), 0)

  const [step, setStep] = useState<ModalStep>('select-mode')
  const [selectedNotebookId, setSelectedNotebookId] = useState<number>(
    defaultNotebookId !== undefined && defaultNotebookId !== null ? defaultNotebookId : 0
  )
  const [localSrs, setLocalSrs] = useState(srsEnabled)

  // Tùy chọn Quizz (Ảnh 3)
  const [quizTypes, setQuizTypes] = useState<string[]>([
    'multiple_choice',
    'listening',
    'writing',
    'matching',
  ])
  const [quizAudioEnabled, setQuizAudioEnabled] = useState(true)

  useEffect(() => {
    if (defaultNotebookId !== undefined && defaultNotebookId !== null) {
      setSelectedNotebookId(defaultNotebookId)
    } else {
      setSelectedNotebookId(0)
    }
  }, [defaultNotebookId, open])

  useEffect(() => {
    setLocalSrs(srsEnabled)
  }, [srsEnabled])

  // Reset về màn hình chọn chế độ khi mở modal
  useEffect(() => {
    if (open) {
      setStep('select-mode')
    }
  }, [open])

  const handleSrsChange = (checked: boolean) => {
    setLocalSrs(checked)
    onToggleSrs?.(checked)
  }

  const handleStartReview = (mode: 'flashcard' | 'quiz' | 'speaking') => {
    const targetId = selectedNotebookId !== undefined && selectedNotebookId !== null ? selectedNotebookId : 0

    onClose()
    if (mode === 'quiz') {
      const typesParam = quizTypes.join(',')
      navigate(
        `/notebooks/${targetId}/review?mode=quiz&srs=${localSrs}&types=${encodeURIComponent(
          typesParam
        )}&audio=${quizAudioEnabled}`
      )
    } else {
      navigate(`/notebooks/${targetId}/review?mode=${mode}&srs=${localSrs}`)
    }
  }

  const toggleQuizType = (typeKey: string) => {
    setQuizTypes((prev) =>
      prev.includes(typeKey)
        ? prev.filter((t) => t !== typeKey)
        : [...prev, typeKey]
    )
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      width={480}
      centered
      destroyOnClose
      styles={{
        body: {
          padding: 12,
        },
      }}
    >
      {/* BƯỚC 1: CHỌN CHẾ ĐỘ LUYỆN TẬP (Ảnh 2 - Bỏ Mini Test) */}
      {step === 'select-mode' && (
        <div className="space-y-5">
          {/* Tiêu đề Modal */}
          <div className="flex items-center justify-between pb-1">
            <h2 className="text-lg font-bold text-gray-900 m-0">
              Chọn chế độ luyện tập
            </h2>
          </div>

          {/* Chọn sổ tay */}
          {notebooks.length > 0 && (
            <div className="flex items-center justify-between gap-3 p-2.5 bg-gray-50 rounded-2xl border border-gray-100 text-xs">
              <span className="font-semibold text-gray-600 shrink-0">Sổ tay ôn tập:</span>
              <Select
                value={selectedNotebookId}
                onChange={setSelectedNotebookId}
                className="w-full text-xs"
                variant="borderless"
                options={[
                  {
                    value: 0,
                    label: `🌟 Tất cả sổ tay (${totalWords} từ)`,
                  },
                  ...notebooks.map((nb) => ({
                    value: nb.id,
                    label: `${nb.name} (${nb.wordCount || 0} từ)`,
                  })),
                ]}
              />
            </div>
          )}

          {/* Hàng Switch Học lặp lại ngắt quãng */}
          <div className="flex items-center justify-between py-2 border-b border-gray-100">
            <span className="text-sm font-medium text-gray-700">
              Học lặp lại ngắt quãng
            </span>
            <Switch
              checked={localSrs}
              onChange={handleSrsChange}
              className={localSrs ? 'bg-blue-600' : ''}
            />
          </div>

          {/* 3 Lựa chọn chế độ (Bỏ Mini Test theo chỉ định) */}
          <div className="space-y-3">
            {/* 1. FlashCard */}
            <div
              onClick={() => handleStartReview('flashcard')}
              className="flex items-center gap-4 p-4 rounded-2xl border border-gray-100 hover:border-blue-300 hover:bg-blue-50/40 bg-gray-50/50 transition-all cursor-pointer group shadow-2xs select-none"
            >
              {/* Icon 2 thẻ lật xanh */}
              <div className="w-12 h-12 rounded-2xl bg-blue-500 text-white flex items-center justify-center shrink-0 shadow-xs group-hover:scale-105 transition-transform">
                <svg
                  className="w-6 h-6 fill-current"
                  viewBox="0 0 24 24"
                >
                  <path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                  FlashCard
                </div>
                <div className="text-xs text-gray-500 mt-0.5 truncate">
                  Ôn tập từ vựng bằng thẻ lật
                </div>
              </div>
            </div>

            {/* 2. Quizz */}
            <div
              onClick={() => setStep('quiz-options')}
              className="flex items-center gap-4 p-4 rounded-2xl border border-gray-100 hover:border-red-300 hover:bg-red-50/40 bg-gray-50/50 transition-all cursor-pointer group shadow-2xs select-none"
            >
              {/* Icon 4 ô vuông đỏ */}
              <div className="w-12 h-12 rounded-2xl bg-red-500 text-white flex items-center justify-center shrink-0 shadow-xs group-hover:scale-105 transition-transform">
                <svg
                  className="w-6 h-6 fill-current"
                  viewBox="0 0 24 24"
                >
                  <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold text-gray-900 group-hover:text-red-600 transition-colors">
                  Quizz
                </div>
                <div className="text-xs text-gray-500 mt-0.5 truncate">
                  Trắc nghiệm nhanh để kiểm tra kiến thức
                </div>
              </div>
            </div>

            {/* 3. Luyện nói, viết */}
            <div
              onClick={() => handleStartReview('speaking')}
              className="flex items-center gap-4 p-4 rounded-2xl border border-gray-100 hover:border-emerald-300 hover:bg-emerald-50/40 bg-gray-50/50 transition-all cursor-pointer group shadow-2xs select-none"
            >
              {/* Icon phát âm và viết xanh lá */}
              <div className="w-12 h-12 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shrink-0 shadow-xs group-hover:scale-105 transition-transform">
                <svg
                  className="w-6 h-6 fill-current"
                  viewBox="0 0 24 24"
                >
                  <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold text-gray-900 group-hover:text-emerald-600 transition-colors">
                  Luyện nói, viết
                </div>
                <div className="text-xs text-gray-500 mt-0.5 truncate">
                  Thực hành phát âm và viết từ
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* BƯỚC 2: TÙY CHỌN QUIZZ (Ảnh 3) */}
      {step === 'quiz-options' && (
        <div className="space-y-4">
          <div>
            <h2 className="text-lg font-bold text-gray-900 m-0">Tùy chọn Quizz</h2>
            <p className="text-xs text-gray-500 mt-1">
              Tuỳ chỉnh dạng câu hỏi, số lượng và cách luyện tập theo mục tiêu của bạn.
            </p>
          </div>

          {/* Chọn dạng bài */}
          <div className="space-y-2">
            <div className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              Chọn dạng bài
            </div>
            <div className="bg-gray-50/70 border border-gray-100 rounded-2xl p-3 space-y-2.5">
              {[
                { key: 'multiple_choice', label: 'Chọn nhiều phương án' },
                { key: 'listening', label: 'Nghe' },
                { key: 'writing', label: 'Luyện viết' },
                { key: 'matching', label: 'Nối' },
              ].map((item) => {
                const isChecked = quizTypes.includes(item.key)
                return (
                  <label
                    key={item.key}
                    onClick={() => toggleQuizType(item.key)}
                    className="flex items-center justify-between text-xs font-medium text-gray-700 cursor-pointer select-none py-1 hover:text-blue-600 transition-colors"
                  >
                    <span>{item.label}</span>
                    <div
                      className={`w-4 h-4 rounded-md border flex items-center justify-center transition-colors ${
                        isChecked
                          ? 'bg-blue-600 border-blue-600 text-white'
                          : 'border-gray-300 bg-white'
                      }`}
                    >
                      {isChecked && <CheckOutlined className="text-[10px]" />}
                    </div>
                  </label>
                )
              })}
            </div>
          </div>

          {/* Cài đặt thêm */}
          <div className="space-y-2">
            <div className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              Cài đặt thêm
            </div>
            <div className="bg-gray-50/70 border border-gray-100 rounded-2xl p-3">
              <label
                onClick={() => setQuizAudioEnabled(!quizAudioEnabled)}
                className="flex items-center justify-between text-xs font-medium text-gray-700 cursor-pointer select-none py-1 hover:text-blue-600 transition-colors"
              >
                <span className="flex items-center gap-1.5">
                  <SoundOutlined /> Âm thanh
                </span>
                <div
                  className={`w-4 h-4 rounded-md border flex items-center justify-center transition-colors ${
                    quizAudioEnabled
                      ? 'bg-blue-600 border-blue-600 text-white'
                      : 'border-gray-300 bg-white'
                  }`}
                >
                  {quizAudioEnabled && <CheckOutlined className="text-[10px]" />}
                </div>
              </label>
            </div>
          </div>

          {/* Footer nút Quay lại & Bắt đầu */}
          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={() => setStep('select-mode')}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 hover:text-gray-900 bg-gray-100 hover:bg-gray-200 transition-colors cursor-pointer"
            >
              <ArrowLeftOutlined className="text-xs" />
              <span>Quay lại</span>
            </button>

            <button
              type="button"
              onClick={() => handleStartReview('quiz')}
              className="px-6 py-2 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-2xs transition-colors cursor-pointer"
            >
              Bắt đầu
            </button>
          </div>
        </div>
      )}
    </Modal>
  )
}
export default ReviewModeSelectModal
