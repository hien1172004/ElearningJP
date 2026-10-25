import React from 'react'
import { HelpCircle } from 'lucide-react'

export interface StandardOptionItem {
  id?: number
  optionKey: number
  optionText: string
  isCorrect: boolean
}

export interface VocabGrammarFormData {
  questionText: string
  options: StandardOptionItem[]
  explanationVi?: string
  points?: number
}

interface VocabGrammarStandardFormProps {
  data: VocabGrammarFormData
  onChange: (updated: VocabGrammarFormData) => void
  disabled?: boolean
  mondaiHint?: string
}

export const VocabGrammarStandardForm: React.FC<VocabGrammarStandardFormProps> = ({
  data,
  onChange,
  disabled = false,
  mondaiHint,
}) => {
  const options = data.options && data.options.length >= 4
    ? data.options
    : [
        { optionKey: 1, optionText: data.options?.[0]?.optionText || '', isCorrect: true },
        { optionKey: 2, optionText: data.options?.[1]?.optionText || '', isCorrect: false },
        { optionKey: 3, optionText: data.options?.[2]?.optionText || '', isCorrect: false },
        { optionKey: 4, optionText: data.options?.[3]?.optionText || '', isCorrect: false },
      ]

  const handleTextChange = (text: string) => {
    onChange({ ...data, questionText: text })
  }

  const handleInsertHelper = (snippet: string) => {
    const cur = data.questionText || ''
    onChange({ ...data, questionText: cur + snippet })
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

  const handleExplanationChange = (val: string) => {
    onChange({ ...data, explanationVi: val })
  }

  return (
    <div className="space-y-4">
      {mondaiHint && (
        <div className="p-3 bg-indigo-50/70 border border-indigo-200/80 rounded-2xl flex items-center gap-2 text-xs text-indigo-900">
          <HelpCircle className="w-4 h-4 text-indigo-600 shrink-0" />
          <span>{mondaiHint}</span>
        </div>
      )}

      {/* Câu hỏi */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <label className="block text-xs font-bold text-slate-800">
            Nội dung câu hỏi: *
          </label>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] text-slate-400">Phím tắt chèn nhanh:</span>
            <button
              type="button"
              disabled={disabled}
              onClick={() => handleInsertHelper(' ____ ')}
              className="px-2 py-0.5 rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 text-[11px] font-mono text-slate-700 cursor-pointer"
            >
              + Gạch dưới ____
            </button>
            <button
              type="button"
              disabled={disabled}
              onClick={() => handleInsertHelper('（　）')}
              className="px-2 py-0.5 rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 text-[11px] font-mono text-slate-700 cursor-pointer"
            >
              + Chỗ trống（　）
            </button>
          </div>
        </div>

        <textarea
          rows={3}
          disabled={disabled}
          value={data.questionText || ''}
          onChange={(e) => handleTextChange(e.target.value)}
          placeholder="Ví dụ: きのう ____ へ いきました。"
          className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
        />
      </div>

      {/* 4 Lựa chọn trả lời */}
      <div className="space-y-2">
        <label className="block text-xs font-bold text-slate-800">
          4 Phương án lựa chọn (Chọn 1 đáp án đúng): *
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
                  title="Chọn làm đáp án đúng"
                >
                  {idx + 1}
                </button>

                <input
                  type="text"
                  disabled={disabled}
                  value={opt.optionText}
                  onChange={(e) => handleOptionChange(idx, e.target.value)}
                  placeholder={`Nội dung đáp án ${idx + 1}...`}
                  className="flex-1 px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
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
                    name="standardCorrectOption"
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
          Giải thích đáp án & cấu trúc từ vựng / ngữ pháp (Tiếng Việt):
        </label>
        <textarea
          rows={2}
          disabled={disabled}
          value={data.explanationVi || ''}
          onChange={(e) => handleExplanationChange(e.target.value)}
          placeholder="Ví dụ: 学校 (がっこう) là cách đọc đúng của trường học. Lựa chọn B sai vì thừa âm..."
          className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
        />
      </div>
    </div>
  )
}
