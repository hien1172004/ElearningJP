import React, { useState } from 'react'
import { BookOpen, Link2 } from 'lucide-react'
import type { TakingPassage } from '@/types'

export interface PassageOptionItem {
  id?: number
  optionKey: number
  optionText: string
  isCorrect: boolean
}

export interface PassageQuestionFormData {
  questionText: string
  passageTitle?: string
  passageContentJp?: string
  passageTranslationVi?: string
  passageImageUrl?: string
  existingPassageId?: number
  options: PassageOptionItem[]
  explanationVi?: string
  points?: number
}

interface PassageQuestionGroupFormProps {
  data: PassageQuestionFormData
  onChange: (updated: PassageQuestionFormData) => void
  disabled?: boolean
  existingPassages?: TakingPassage[]
  isClozeText?: boolean // Mondai 3 Text grammar (19-23)
}

export const PassageQuestionGroupForm: React.FC<PassageQuestionGroupFormProps> = ({
  data,
  onChange,
  disabled = false,
  existingPassages = [],
  isClozeText = false,
}) => {
  const [passageMode, setPassageMode] = useState<'NEW' | 'EXISTING'>(
    data.existingPassageId ? 'EXISTING' : 'NEW'
  )

  const options = data.options && data.options.length >= 4
    ? data.options
    : [
        { optionKey: 1, optionText: data.options?.[0]?.optionText || '', isCorrect: true },
        { optionKey: 2, optionText: data.options?.[1]?.optionText || '', isCorrect: false },
        { optionKey: 3, optionText: data.options?.[2]?.optionText || '', isCorrect: false },
        { optionKey: 4, optionText: data.options?.[3]?.optionText || '', isCorrect: false },
      ]

  const handleSelectExistingPassage = (passageId: number) => {
    const found = existingPassages.find((p) => p.id === passageId)
    if (found) {
      onChange({
        ...data,
        existingPassageId: found.id,
        passageTitle: found.title,
        passageContentJp: found.contentJp,
        passageTranslationVi: found.translationVi || '',
        passageImageUrl: found.imageUrl || '',
      })
    }
  }

  const handleInsertBlank = (blankTag: string) => {
    const cur = data.passageContentJp || ''
    onChange({
      ...data,
      passageContentJp: cur + ` ${blankTag} `,
    })
  }

  const handleOptionChange = (index: number, val: string) => {
    const nextOptions = [...options]
    nextOptions[index] = { ...nextOptions[index], optionText: val }
    onChange({ ...data, options: nextOptions })
  }

  const handleSelectCorrect = (index: number) => {
    const nextOptions = options.map((opt, idx) => ({
      ...opt,
      isCorrect: idx === index,
    }))
    onChange({ ...data, options: nextOptions })
  }

  return (
    <div className="space-y-5">
      {/* KHỐI 1: QUẢN LÝ BÀI ĐỌC DÙNG CHUNG (PASSAGE) */}
      <div className="p-4 bg-sky-50/80 border border-sky-200 rounded-3xl space-y-3.5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="font-extrabold text-sky-950 flex items-center gap-1.5 text-xs">
            <BookOpen className="w-4 h-4 text-sky-600" />
            <span>Đoạn văn bài đọc dùng chung (Shared Reading Passage)</span>
          </span>

          {existingPassages.length > 0 && (
            <div className="flex items-center gap-1 bg-white p-1 rounded-xl border border-sky-200 text-xs">
              <button
                type="button"
                disabled={disabled}
                onClick={() => setPassageMode('NEW')}
                className={`px-2.5 py-1 rounded-lg font-bold transition cursor-pointer ${
                  passageMode === 'NEW'
                    ? 'bg-sky-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                + Soạn bài đọc mới
              </button>
              <button
                type="button"
                disabled={disabled}
                onClick={() => setPassageMode('EXISTING')}
                className={`px-2.5 py-1 rounded-lg font-bold flex items-center gap-1 transition cursor-pointer ${
                  passageMode === 'EXISTING'
                    ? 'bg-sky-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <Link2 className="w-3.5 h-3.5" />
                <span>Dùng chung bài đọc đã có</span>
              </button>
            </div>
          )}
        </div>

        {/* Chế độ chọn bài đọc đã có trong Section để liên kết nhiều câu hỏi */}
        {passageMode === 'EXISTING' && existingPassages.length > 0 ? (
          <div className="space-y-2 bg-white p-3 rounded-2xl border border-sky-200">
            <label className="block text-[11px] font-bold text-slate-700">
              Chọn đoạn văn đã có để liên kết câu hỏi này vào:
            </label>
            <select
              value={data.existingPassageId || ''}
              onChange={(e) => handleSelectExistingPassage(Number(e.target.value))}
              className="w-full p-2 border border-slate-300 rounded-xl bg-white font-medium text-xs text-slate-800"
            >
              <option value="">-- Chọn bài đọc đang có trong đề --</option>
              {existingPassages.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.title} ({p.contentJp ? p.contentJp.slice(0, 45) + '...' : 'Tài liệu ảnh'})
                </option>
              ))}
            </select>
            {data.passageContentJp && (
              <div className="mt-2 p-2.5 bg-slate-50 rounded-xl font-serif text-xs text-slate-700 line-clamp-3">
                {data.passageContentJp}
              </div>
            )}
          </div>
        ) : (
          /* Chế độ soạn thảo bài đọc mới */
          <div className="space-y-3">
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">
                Tiêu đề đoạn văn: *
              </label>
              <input
                type="text"
                disabled={disabled}
                value={data.passageTitle || ''}
                onChange={(e) => onChange({ ...data, passageTitle: e.target.value })}
                placeholder={isClozeText ? 'Ví dụ: Bài đục lỗ ngữ pháp (19) - (23)' : 'Ví dụ: Đoạn văn ngắn 1 hoặc Bài đọc trung'}
                className="w-full p-2.5 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-[11px] font-bold text-slate-700">
                  Nội dung tiếng Nhật của bài đọc: *
                </label>
                {isClozeText && (
                  <div className="flex items-center gap-1">
                    <span className="text-[10px] text-slate-400">Chèn chỗ trống:</span>
                    {['(19)', '(20)', '(21)', '(22)', '(23)'].map((tag) => (
                      <button
                        key={tag}
                        type="button"
                        onClick={() => handleInsertBlank(tag)}
                        className="px-1.5 py-0.5 bg-white border border-sky-300 rounded-md text-[10px] font-mono text-sky-800 hover:bg-sky-100 cursor-pointer"
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <textarea
                rows={5}
                disabled={disabled}
                value={data.passageContentJp || ''}
                onChange={(e) => onChange({ ...data, passageContentJp: e.target.value })}
                placeholder="Dán nội dung đoạn văn bài đọc hoặc bài văn đục lỗ vào đây..."
                className="w-full p-2.5 text-xs border border-slate-300 rounded-xl font-serif bg-white leading-relaxed focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">
                Bản dịch tiếng Việt tham khảo (tùy chọn):
              </label>
              <textarea
                rows={2}
                disabled={disabled}
                value={data.passageTranslationVi || ''}
                onChange={(e) => onChange({ ...data, passageTranslationVi: e.target.value })}
                placeholder="Dịch nghĩa đoạn văn để hỗ trợ học viên xem giải thích sau khi thi..."
                className="w-full p-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
              />
            </div>
          </div>
        )}
      </div>

      {/* KHỐI 2: CÂU HỎI CON LIÊN KẾT ĐOẠN VĂN TRÊN */}
      <div className="space-y-4">
        <div className="space-y-1.5">
          <label className="block text-xs font-bold text-slate-800">
            Câu hỏi con (Ví dụ: &quot;(19)に入る最もよいものはどれか&quot; hoặc &quot;この文章で筆者が最も言いたいことは何か&quot;): *
          </label>
          <input
            type="text"
            disabled={disabled}
            value={data.questionText || ''}
            onChange={(e) => onChange({ ...data, questionText: e.target.value })}
            placeholder="Nội dung câu hỏi con..."
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
          />
        </div>

        {/* 4 Lựa chọn cho câu hỏi này */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-800">
            4 Phương án trả lời cho câu hỏi này: *
          </label>
          <div className="space-y-2">
            {options.map((opt, idx) => {
              const isCorrect = opt.isCorrect
              return (
                <div
                  key={opt.optionKey}
                  className={`p-3 rounded-2xl border transition flex items-center gap-3 ${
                    isCorrect
                      ? 'bg-emerald-50/70 border-emerald-300 ring-1 ring-emerald-200'
                      : 'bg-white border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => handleSelectCorrect(idx)}
                    className={`w-7 h-7 rounded-xl flex items-center justify-center font-black text-xs shrink-0 cursor-pointer transition ${
                      isCorrect
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {idx + 1}
                  </button>

                  <input
                    type="text"
                    disabled={disabled}
                    value={opt.optionText}
                    onChange={(e) => handleOptionChange(idx, e.target.value)}
                    placeholder={`Nội dung lựa chọn ${idx + 1}...`}
                    className="flex-1 px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
                  />

                  <label
                    onClick={() => !disabled && handleSelectCorrect(idx)}
                    className={`flex items-center gap-1.5 text-xs font-bold shrink-0 cursor-pointer px-2.5 py-1 rounded-xl transition ${
                      isCorrect
                        ? 'text-emerald-700 bg-emerald-100/80 font-black'
                        : 'text-slate-400 hover:text-slate-600'
                    }`}
                  >
                    <input
                      type="radio"
                      name="passageCorrectOption"
                      disabled={disabled}
                      checked={isCorrect}
                      onChange={() => handleSelectCorrect(idx)}
                      className="w-4 h-4 text-emerald-600 accent-emerald-600 cursor-pointer"
                    />
                    <span>{isCorrect ? 'Đáp án đúng' : 'Chọn đúng'}</span>
                  </label>
                </div>
              )
            })}
          </div>
        </div>

        {/* Lời giải thích */}
        <div className="space-y-1">
          <label className="block text-xs font-bold text-slate-700">
            Giải thích đáp án cho câu hỏi này:
          </label>
          <textarea
            rows={2}
            disabled={disabled}
            value={data.explanationVi || ''}
            onChange={(e) => onChange({ ...data, explanationVi: e.target.value })}
            placeholder="Dẫn chứng trong đoạn văn vì sao lựa chọn này đúng..."
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-sky-500/20 focus:outline-none"
          />
        </div>
      </div>
    </div>
  )
}
