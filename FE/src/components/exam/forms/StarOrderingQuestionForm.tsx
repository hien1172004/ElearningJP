import React from 'react'
import { Star, Sparkles } from 'lucide-react'

export interface StarOptionItem {
  id?: number
  optionKey: number
  optionText: string
  isCorrect: boolean
}

export interface StarOrderingFormData {
  questionText: string
  options: StarOptionItem[]
  starPosition: number // 1, 2, 3, 4
  explanationVi?: string
  points?: number
}

interface StarOrderingQuestionFormProps {
  data: StarOrderingFormData
  onChange: (updated: StarOrderingFormData) => void
  disabled?: boolean
}

export const StarOrderingQuestionForm: React.FC<StarOrderingQuestionFormProps> = ({
  data,
  onChange,
  disabled = false,
}) => {
  // Đảm bảo luôn có đúng 4 options
  const options = data.options && data.options.length === 4
    ? data.options
    : [
        { optionKey: 1, optionText: data.options?.[0]?.optionText || '', isCorrect: true },
        { optionKey: 2, optionText: data.options?.[1]?.optionText || '', isCorrect: false },
        { optionKey: 3, optionText: data.options?.[2]?.optionText || '', isCorrect: false },
        { optionKey: 4, optionText: data.options?.[3]?.optionText || '', isCorrect: false },
      ]

  const starPos = data.starPosition && data.starPosition >= 1 && data.starPosition <= 4 ? data.starPosition : 3

  const handleTextChange = (text: string) => {
    onChange({ ...data, questionText: text })
  }

  const handleOptionChange = (index: number, val: string) => {
    const nextOptions = [...options]
    nextOptions[index] = { ...nextOptions[index], optionText: val }
    onChange({ ...data, options: nextOptions })
  }

  const handleStarPositionChange = (pos: number) => {
    // Trong đề thi JLPT, thí sinh chọn đáp án rơi vào vị trí dấu sao
    // Vì vậy option tại vị trí dấu sao chính là đáp án cần chọn
    const nextOptions = options.map((opt, idx) => ({
      ...opt,
      isCorrect: idx + 1 === pos,
    }))
    onChange({ ...data, starPosition: pos, options: nextOptions })
  }

  const handleExplanationChange = (val: string) => {
    onChange({ ...data, explanationVi: val })
  }

  return (
    <div className="space-y-4">
      {/* Banner hướng dẫn chuẩn JLPT cho câu hỏi dấu sao */}
      <div className="p-3 bg-amber-50/80 border border-amber-200/90 rounded-2xl flex items-start gap-2.5">
        <Sparkles className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="text-xs text-amber-950 space-y-1">
          <p className="font-extrabold">Đặc thù dạng bài Sắp xếp câu (文の組み立て):</p>
          <p className="text-amber-800 leading-relaxed">
            Thí sinh sắp xếp 4 cụm từ xáo trộn vào 4 vị trí gạch dưới. Đáp án đúng là cụm từ nằm tại vị trí có gắn dấu sao <strong className="text-amber-600 font-black">★</strong>.
          </p>
        </div>
      </div>

      {/* Câu hỏi hoặc ngữ cảnh đầu câu */}
      <div className="space-y-1.5">
        <label className="block text-xs font-bold text-slate-800">
          Câu đề bài (Ví dụ: 毎朝、私は ____ ____ ★ ____ 学校へ行きます。): *
        </label>
        <textarea
          rows={2}
          disabled={disabled}
          value={data.questionText || ''}
          onChange={(e) => handleTextChange(e.target.value)}
          placeholder="Ví dụ: 田中さんは ____ ____ ★ ____ 行きました。"
          className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-amber-500/20 focus:outline-none"
        />
      </div>

      {/* Khung mô phỏng trực quan thứ tự 4 vị trí gạch dưới */}
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl space-y-2">
        <span className="text-[11px] font-bold text-slate-700 flex items-center gap-1.5">
          <Star className="w-4 h-4 text-amber-500 fill-amber-400" />
          <span>Vị trí đặt dấu sao ★ (Chọn vị trí 1, 2, 3 hoặc 4 để gắn dấu sao):</span>
        </span>

        <div className="grid grid-cols-4 gap-2 pt-1">
          {[1, 2, 3, 4].map((pos) => {
            const isStar = starPos === pos
            return (
              <button
                key={pos}
                type="button"
                disabled={disabled}
                onClick={() => handleStarPositionChange(pos)}
                className={`py-2 px-3 rounded-xl border flex flex-col items-center justify-center gap-1 transition cursor-pointer ${
                  isStar
                    ? 'bg-amber-500 text-white border-amber-600 shadow-sm font-extrabold ring-2 ring-amber-300'
                    : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-100 font-bold'
                }`}
              >
                <div className="flex items-center gap-1 text-xs">
                  <span>Vị trí {pos}</span>
                  {isStar && <Star className="w-3.5 h-3.5 fill-white text-white" />}
                </div>
                <span className="text-[10px] opacity-80">
                  {isStar ? '★ Dấu sao' : 'Gạch dưới'}
                </span>
              </button>
            )
          })}
        </div>

        {/* Live Preview dòng câu hoàn chỉnh */}
        <div className="mt-3 p-3 bg-white border border-amber-200/80 rounded-xl text-center font-serif text-sm text-slate-900 tracking-wide shadow-2xs">
          <span className="text-slate-400 font-sans text-[11px] block mb-1">Mô phỏng hiển thị trên đề thi:</span>
          <span>
            {data.questionText ? (
              data.questionText
            ) : (
              <span>
                (Câu bắt đầu)&nbsp;&nbsp;
                <span className="underline decoration-slate-400 font-sans">{starPos === 1 ? ' ★ ' : ' ___ '}</span>&nbsp;
                <span className="underline decoration-slate-400 font-sans">{starPos === 2 ? ' ★ ' : ' ___ '}</span>&nbsp;
                <span className="underline decoration-slate-400 font-sans">{starPos === 3 ? ' ★ ' : ' ___ '}</span>&nbsp;
                <span className="underline decoration-slate-400 font-sans">{starPos === 4 ? ' ★ ' : ' ___ '}</span>&nbsp;
                (Câu kết thúc)
              </span>
            )}
          </span>
        </div>
      </div>

      {/* 4 Ô nhập liệu cụm từ xáo trộn */}
      <div className="space-y-2">
        <label className="block text-xs font-bold text-slate-800">
          4 Cụm từ/Từ ngữ xáo trộn (Thí sinh sắp xếp 1, 2, 3, 4): *
        </label>
        <div className="space-y-2">
          {options.map((opt, idx) => {
            const isStarSlot = starPos === idx + 1
            return (
              <div
                key={opt.optionKey}
                className={`p-3 rounded-2xl border transition flex items-center gap-3 ${
                  isStarSlot
                    ? 'bg-amber-50/70 border-amber-300 ring-1 ring-amber-200'
                    : 'bg-white border-slate-200'
                }`}
              >
                <div className="flex items-center gap-1.5 shrink-0">
                  <span
                    className={`w-7 h-7 rounded-xl flex items-center justify-center font-black text-xs ${
                      isStarSlot
                        ? 'bg-amber-500 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {idx + 1}
                  </span>
                  {isStarSlot && (
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-amber-200 text-amber-900 flex items-center gap-0.5">
                      <Star className="w-3 h-3 fill-amber-700 text-amber-700" /> Vị trí ★
                    </span>
                  )}
                </div>

                <input
                  type="text"
                  disabled={disabled}
                  value={opt.optionText}
                  onChange={(e) => handleOptionChange(idx, e.target.value)}
                  placeholder={`Cụm từ lựa chọn ${idx + 1}...`}
                  className="flex-1 px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-amber-500/20 focus:outline-none"
                />

                <label className="flex items-center gap-1.5 text-xs font-bold text-slate-600 cursor-pointer shrink-0">
                  <input
                    type="radio"
                    name="starCorrectOption"
                    disabled={disabled}
                    checked={opt.isCorrect}
                    onChange={() => {
                      const next = options.map((o, i) => ({ ...o, isCorrect: i === idx }))
                      onChange({ ...data, options: next, starPosition: idx + 1 })
                    }}
                    className="w-4 h-4 text-amber-600 accent-amber-600 cursor-pointer"
                  />
                  <span className="text-[11px]">Đáp án đúng (★)</span>
                </label>
              </div>
            )
          })}
        </div>
      </div>

      {/* Lời giải thích tiếng Việt */}
      <div className="space-y-1">
        <label className="block text-xs font-bold text-slate-700">
          Giải thích thứ tự sắp xếp & dịch nghĩa câu:
        </label>
        <textarea
          rows={2}
          disabled={disabled}
          value={data.explanationVi || ''}
          onChange={(e) => handleExplanationChange(e.target.value)}
          placeholder="Ví dụ: Thứ tự đúng là 4 - 1 - 3 - 2. Vị trí dấu sao là số 3..."
          className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
        />
      </div>
    </div>
  )
}
