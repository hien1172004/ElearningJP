import React, { useState, useRef, useCallback } from 'react'
import {
  Bold,
  Underline,
  Sparkles,
  Eye,
  EyeOff,
  Check,
  X,
  Star,
  Brackets,
} from 'lucide-react'

export interface JapaneseTextEditorProps {
  value: string
  onChange: (value: string) => void
  label?: string
  placeholder?: string
  rows?: number
  disabled?: boolean
  className?: string
  multiline?: boolean
  compact?: boolean
  showStarButton?: boolean
  showClozeButton?: boolean
  helperText?: string
}

export const JapaneseTextEditor: React.FC<JapaneseTextEditorProps> = ({
  value = '',
  onChange,
  label,
  placeholder,
  rows = 2,
  disabled = false,
  className = '',
  multiline = true,
  compact = false,
  showStarButton = false,
  showClozeButton = false,
  helperText,
}) => {
  const [showPreview, setShowPreview] = useState(false)
  const [showFuriganaModal, setShowFuriganaModal] = useState(false)
  const [kanjiInput, setKanjiInput] = useState('')
  const [readingInput, setReadingInput] = useState('')

  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const readingFieldRef = useRef<HTMLInputElement>(null)

  // Lưu lại vị trí bôi đen để thay thế chính xác
  const selectionRangeRef = useRef<{ start: number; end: number }>({ start: 0, end: 0 })

  const getElement = useCallback(() => {
    return multiline ? textareaRef.current : inputRef.current
  }, [multiline])

  // Chèn hoặc bọc thẻ xung quanh đoạn văn bản được chọn
  const wrapSelection = useCallback(
    (tagOpen: string, tagClose: string, defaultPlaceholder = '') => {
      const el = getElement()
      if (!el || disabled) return

      const start = el.selectionStart ?? 0
      const end = el.selectionEnd ?? 0
      const text = el.value || ''

      const selected = text.substring(start, end)
      const content = selected || defaultPlaceholder
      const newText = text.substring(0, start) + tagOpen + content + tagClose + text.substring(end)

      onChange(newText)

      setTimeout(() => {
        el.focus()
        const newCursorPos = start + tagOpen.length + content.length + tagClose.length
        el.setSelectionRange(newCursorPos, newCursorPos)
      }, 0)
    },
    [getElement, disabled, onChange]
  )

  // Mở popup nhập Furigana
  const handleOpenFurigana = () => {
    if (disabled) return
    const el = getElement()
    if (!el) return

    const start = el.selectionStart ?? 0
    const end = el.selectionEnd ?? 0
    const selected = (el.value || '').substring(start, end).trim()

    selectionRangeRef.current = { start, end }
    setKanjiInput(selected)
    setReadingInput('')
    setShowFuriganaModal(true)

    setTimeout(() => {
      readingFieldRef.current?.focus()
    }, 50)
  }

  // Áp dụng Furigana <ruby>漢字<rt>furigana</rt></ruby>
  const handleApplyFurigana = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!kanjiInput.trim() || !readingInput.trim()) return

    const el = getElement()
    const text = value || ''
    const { start, end } = selectionRangeRef.current

    const rubyTag = `<ruby>${kanjiInput.trim()}<rt>${readingInput.trim()}</rt></ruby>`

    let newText = ''
    if (start !== undefined && end !== undefined && start !== end) {
      newText = text.substring(0, start) + rubyTag + text.substring(end)
    } else if (start !== undefined) {
      newText = text.substring(0, start) + rubyTag + text.substring(start)
    } else {
      newText = text + rubyTag
    }

    onChange(newText)
    setShowFuriganaModal(false)
    setKanjiInput('')
    setReadingInput('')

    setTimeout(() => {
      el?.focus()
    }, 0)
  }

  const insertTextAtCursor = (insertStr: string) => {
    wrapSelection(insertStr, '', '')
  }

  // --- RENDERING CHẾ ĐỘ GỌN (COMPACT / SINGLE-LINE CHO OPTIONS) ---
  if (compact || !multiline) {
    return (
      <div className={`relative ${className}`}>
        <div className="relative flex items-center">
          <input
            ref={inputRef}
            type="text"
            value={value}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            className={`w-full py-2 pl-3 pr-28 border rounded-xl text-xs font-serif transition focus:outline-none ${
              disabled
                ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
                : 'bg-white border-slate-300 hover:border-indigo-400 focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/20 shadow-2xs'
            }`}
          />

          {/* Micro Toolbar đính kèm ngay mép phải ô input */}
          {!disabled && (
            <div className="absolute right-1.5 flex items-center gap-0.5 bg-slate-50/90 backdrop-blur-xs p-0.5 rounded-lg border border-slate-200 shadow-2xs">
              <button
                type="button"
                onClick={() => wrapSelection('<b>', '</b>', 'đậm')}
                title="In đậm (<b>)"
                className="w-5 h-5 flex items-center justify-center rounded hover:bg-slate-200 text-slate-700 font-bold transition cursor-pointer"
              >
                <Bold className="w-2.5 h-2.5" />
              </button>
              <button
                type="button"
                onClick={() => wrapSelection('<u>', '</u>', 'gạch chân')}
                title="Gạch chân (<u>)"
                className="w-5 h-5 flex items-center justify-center rounded hover:bg-slate-200 text-slate-700 font-bold transition cursor-pointer"
              >
                <Underline className="w-2.5 h-2.5" />
              </button>
              <button
                type="button"
                onClick={handleOpenFurigana}
                title="Thêm Furigana (<ruby>)"
                className="px-1.5 h-5 flex items-center gap-0.5 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold transition cursor-pointer border border-indigo-200/80 text-[10px]"
              >
                <Sparkles className="w-2.5 h-2.5 text-indigo-600" />
                <span>ルビ</span>
              </button>
              {value && (
                <button
                  type="button"
                  onClick={() => setShowPreview(!showPreview)}
                  title={showPreview ? 'Ẩn xem thử' : 'Xem thử render'}
                  className={`w-5 h-5 flex items-center justify-center rounded transition cursor-pointer ${
                    showPreview ? 'bg-emerald-100 text-emerald-800' : 'hover:bg-slate-200 text-slate-500'
                  }`}
                >
                  {showPreview ? <EyeOff className="w-2.5 h-2.5" /> : <Eye className="w-2.5 h-2.5" />}
                </button>
              )}
            </div>
          )}
        </div>

        {/* Popover Furigana nổi */}
        {showFuriganaModal && (
          <div className="absolute top-full left-0 mt-1.5 z-50 w-72 sm:w-80 p-2.5 bg-white border border-indigo-200 rounded-xl shadow-xl space-y-2 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between text-xs font-bold text-indigo-950 border-b border-indigo-100 pb-1.5">
              <span className="flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                Thêm Furigana (ルビ)
              </span>
              <button
                type="button"
                onClick={() => setShowFuriganaModal(false)}
                className="p-0.5 text-slate-400 hover:text-slate-600 rounded"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <form onSubmit={handleApplyFurigana} className="space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-semibold text-slate-600 mb-0.5">Hán tự:</label>
                  <input
                    type="text"
                    value={kanjiInput}
                    onChange={(e) => setKanjiInput(e.target.value)}
                    placeholder="漢字"
                    className="w-full p-1.5 border border-slate-300 rounded-lg text-xs font-serif bg-slate-50 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-600 mb-0.5">Cách đọc:</label>
                  <input
                    ref={readingFieldRef}
                    type="text"
                    value={readingInput}
                    onChange={(e) => setReadingInput(e.target.value)}
                    placeholder="かんじ"
                    className="w-full p-1.5 border border-slate-300 rounded-lg text-xs font-serif bg-slate-50 focus:bg-white"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between pt-1 border-t border-slate-100">
                <div className="text-xs font-serif text-slate-800">
                  {kanjiInput && readingInput ? (
                    <ruby>
                      {kanjiInput}
                      <rt>{readingInput}</rt>
                    </ruby>
                  ) : (
                    <span className="text-[10px] text-slate-400 italic font-sans">Nhập 2 ô trên</span>
                  )}
                </div>

                <div className="flex gap-1">
                  <button
                    type="button"
                    onClick={() => setShowFuriganaModal(false)}
                    className="px-2 py-0.5 text-[10px] text-slate-500 hover:bg-slate-100 rounded"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={!kanjiInput.trim() || !readingInput.trim()}
                    className="px-2.5 py-1 text-[11px] bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg shadow-2xs transition disabled:opacity-50"
                  >
                    Chèn
                  </button>
                </div>
              </div>
            </form>
          </div>
        )}

        {/* Live Preview nếu bật */}
        {showPreview && value && (
          <div className="mt-1 p-2 bg-indigo-50/50 border border-indigo-100 rounded-lg text-xs font-serif text-slate-900 leading-relaxed">
            <span className="text-[9px] font-sans text-indigo-400 font-bold block uppercase mb-0.5">Xem thử:</span>
            <div dangerouslySetInnerHTML={{ __html: value }} />
          </div>
        )}
      </div>
    )
  }

  // --- RENDERING CHẾ ĐỘ FULL (MULTILINE CHO CÂU HỎI / SCRIPTS / BÀI ĐỌC) ---
  return (
    <div className={`space-y-1.5 ${className}`}>
      {/* Header: Label & Thanh Toolbar */}
      {(label || !disabled) && (
        <div className="flex items-center justify-between">
          {label && (
            <label className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
              <span>{label}</span>
            </label>
          )}

          {!disabled && (
            <div className="flex items-center gap-1 text-[11px]">
              <button
                type="button"
                onClick={() => wrapSelection('<b>', '</b>', 'đậm')}
                title="In đậm chữ (<b>...</b>)"
                className="p-1 px-1.5 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold transition flex items-center gap-0.5 cursor-pointer"
              >
                <Bold className="w-3 h-3" />
                <span className="text-[10px]">B</span>
              </button>

              <button
                type="button"
                onClick={() => wrapSelection('<u>', '</u>', 'gạch chân')}
                title="Gạch chân chữ JLPT (<u>...</u>)"
                className="p-1 px-1.5 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold transition flex items-center gap-0.5 cursor-pointer"
              >
                <Underline className="w-3 h-3" />
                <span className="text-[10px]">U</span>
              </button>

              <button
                type="button"
                onClick={handleOpenFurigana}
                title="Thêm Furigana (<ruby>漢字<rt>cách đọc</rt></ruby>)"
                className="p-1 px-2 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold transition flex items-center gap-1 cursor-pointer border border-indigo-200/80 shadow-2xs"
              >
                <Sparkles className="w-3 h-3 text-indigo-600" />
                <span className="text-[10px]">ルビ Furigana</span>
              </button>

              {showStarButton && (
                <button
                  type="button"
                  onClick={() => insertTextAtCursor('＿＿＿　＿＿＿　＿★＿　＿＿＿')}
                  title="Chèn mẫu câu hỏi sắp xếp sao ★"
                  className="p-1 px-1.5 rounded bg-amber-50 hover:bg-amber-100 text-amber-700 font-bold transition flex items-center gap-0.5 cursor-pointer border border-amber-200"
                >
                  <Star className="w-3 h-3 fill-amber-500 text-amber-600" />
                  <span className="text-[10px]">Dấu sao</span>
                </button>
              )}

              {showClozeButton && (
                <button
                  type="button"
                  onClick={() => wrapSelection('【 ', ' 】', '21')}
                  title="Chèn ô trống dạng bài đục lỗ (【 21 】)"
                  className="p-1 px-1.5 rounded bg-sky-50 hover:bg-sky-100 text-sky-700 font-bold transition flex items-center gap-0.5 cursor-pointer border border-sky-200"
                >
                  <Brackets className="w-3 h-3 text-sky-600" />
                  <span className="text-[10px]">【 】</span>
                </button>
              )}

              <button
                type="button"
                onClick={() => setShowPreview(!showPreview)}
                title={showPreview ? 'Đóng xem trước' : 'Xem trước cách hiển thị'}
                className={`p-1 px-1.5 rounded transition flex items-center gap-1 cursor-pointer ${
                  showPreview
                    ? 'bg-emerald-100 text-emerald-800 font-bold'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                }`}
              >
                {showPreview ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
                <span className="text-[10px]">{showPreview ? 'Ẩn xem thử' : 'Xem thử'}</span>
              </button>
            </div>
          )}
        </div>
      )}

      {/* Popover Furigana cho Multiline */}
      {showFuriganaModal && (
        <div className="p-2.5 bg-indigo-50/90 border border-indigo-200 rounded-xl space-y-2 shadow-xs animate-in fade-in duration-150">
          <div className="flex items-center justify-between text-xs font-bold text-indigo-950">
            <span className="flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
              Thêm Furigana (ルビ) cho chữ Hán:
            </span>
            <button
              type="button"
              onClick={() => setShowFuriganaModal(false)}
              className="p-0.5 text-slate-400 hover:text-slate-600 rounded"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          <form onSubmit={handleApplyFurigana} className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <div>
              <label className="block text-[10px] font-semibold text-slate-600 mb-0.5">Chữ Hán (Kanji):</label>
              <input
                type="text"
                value={kanjiInput}
                onChange={(e) => setKanjiInput(e.target.value)}
                placeholder="Ví dụ: 体育館"
                className="w-full p-1.5 border border-indigo-200 rounded-lg text-xs font-serif bg-white"
              />
            </div>
            <div>
              <label className="block text-[10px] font-semibold text-slate-600 mb-0.5">Cách đọc (Hiragana):</label>
              <input
                ref={readingFieldRef}
                type="text"
                value={readingInput}
                onChange={(e) => setReadingInput(e.target.value)}
                placeholder="Ví dụ: たいいくかん"
                className="w-full p-1.5 border border-indigo-200 rounded-lg text-xs font-serif bg-white"
              />
            </div>

            <div className="sm:col-span-2 flex items-center justify-between pt-1">
              <div className="text-xs font-serif text-slate-700 bg-white/80 px-2 py-1 rounded border border-indigo-100 flex items-center gap-1.5">
                <span className="text-[10px] text-slate-400 font-sans">Hiển thị:</span>
                {kanjiInput && readingInput ? (
                  <ruby>
                    {kanjiInput}
                    <rt>{readingInput}</rt>
                  </ruby>
                ) : (
                  <span className="text-slate-400 italic text-[11px] font-sans">Nhập cả 2 ô trên</span>
                )}
              </div>

              <div className="flex gap-1.5">
                <button
                  type="button"
                  onClick={() => setShowFuriganaModal(false)}
                  className="px-2.5 py-1 rounded-lg border border-slate-300 text-slate-600 text-[11px] font-semibold hover:bg-white cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={!kanjiInput.trim() || !readingInput.trim()}
                  className="px-3 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-[11px] font-bold shadow-2xs transition disabled:opacity-50 cursor-pointer flex items-center gap-1"
                >
                  <Check className="w-3 h-3" />
                  <span>Áp dụng</span>
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* Ô TextArea */}
      <textarea
        ref={textareaRef}
        rows={rows}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className={`w-full p-2.5 border rounded-xl text-xs font-serif leading-relaxed transition ${
          disabled
            ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
            : 'bg-white border-slate-300 hover:border-indigo-400 focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/20 shadow-2xs'
        }`}
      />

      {/* Live Preview nếu bật */}
      {showPreview && value && (
        <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
          <span className="text-[10px] font-bold text-slate-400 block uppercase tracking-wider">
            Xem trước khi hiển thị (Live Preview):
          </span>
          <div
            className="text-xs font-serif text-slate-900 leading-relaxed bg-white p-2.5 rounded-lg border border-slate-100 shadow-2xs"
            dangerouslySetInnerHTML={{ __html: value }}
          />
        </div>
      )}

      {helperText && <p className="text-[11px] text-slate-400 italic">{helperText}</p>}
    </div>
  )
}

export default JapaneseTextEditor
