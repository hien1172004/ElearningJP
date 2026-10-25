import React, { useState, useEffect, useRef, useCallback } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import {
  Bold as BoldIcon,
  Underline as UnderlineIcon,
  RotateCcw,
  RotateCw,
  RemoveFormatting,
  Sparkles,
  User,
  Hash,
  Star,
  Brackets,
} from 'lucide-react'

import { RubyExtension } from './extensions/RubyExtension'
import { SpeakerExtension } from './extensions/SpeakerExtension'
import { FuriganaPopover } from './components/FuriganaPopover'
import { sanitizeJapaneseHtml, normalizeLegacy } from './utils/sanitize'

export interface RichTextFieldProps {
  value: string
  onChange: (html: string) => void
  variant?: 'multiline' | 'singleline'
  label?: string
  placeholder?: string
  disabled?: boolean
  className?: string
  minRows?: number
  enableFurigana?: boolean
  enableSpeakerLabel?: boolean
  showStarButton?: boolean
  showClozeButton?: boolean
  helperText?: string
}

export const RichTextField: React.FC<RichTextFieldProps> = ({
  value = '',
  onChange,
  variant = 'multiline',
  label,
  placeholder = 'Nhập văn bản tiếng Nhật...',
  disabled = false,
  className = '',
  minRows = 3,
  enableFurigana = true,
  enableSpeakerLabel = false,
  showStarButton = false,
  showClozeButton = false,
  helperText,
}) => {
  const isSingleLine = variant === 'singleline'

  // Popover state
  const [popoverOpen, setPopoverOpen] = useState(false)
  const [popoverBase, setPopoverBase] = useState('')
  const [popoverReading, setPopoverReading] = useState('')
  const [popoverPos, setPopoverPos] = useState<{ top: number; left: number } | undefined>(undefined)
  const [isEditingExistingRuby, setIsEditingExistingRuby] = useState(false)
  const activeRubyPosRef = useRef<{ from: number; to: number } | null>(null)

  // Speaker label dropdown state
  const [showSpeakerMenu, setShowSpeakerMenu] = useState(false)

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: false,
        codeBlock: false,
        blockquote: false,
        bulletList: false,
        orderedList: false,
        horizontalRule: false,
      }),
      Underline,
      RubyExtension,
      SpeakerExtension,
      Placeholder.configure({
        placeholder,
        emptyEditorClass: 'is-editor-empty',
      }),
      CharacterCount,
    ],
    content: normalizeLegacy(value),
    editable: !disabled,
    editorProps: {
      attributes: {
        class: `prose max-w-none focus:outline-none font-serif text-slate-800 leading-[2.2] select-text ${
          isSingleLine ? 'min-h-[36px] py-1 px-2.5' : 'p-3'
        }`,
      },
      handleKeyDown: (view, event) => {
        // Chặn Enter nếu là singleline (dành cho các ô đáp án 1 dòng)
        if (isSingleLine && event.key === 'Enter') {
          event.preventDefault()
          return true
        }

        // Phím tắt Ctrl+Shift+R mở Furigana
        if (event.ctrlKey && event.shiftKey && (event.key === 'R' || event.key === 'r')) {
          event.preventDefault()
          triggerFuriganaFromSelection()
          return true
        }

        return false
      },
      handleClickOn: (view, pos, node) => {
        if (node.type.name === 'ruby') {
          // Bấm vào node ruby để mở popover sửa/xóa
          const dom = view.nodeDOM(pos) as HTMLElement
          const rect = dom?.getBoundingClientRect()
          setPopoverBase(node.attrs.base || '')
          setPopoverReading(node.attrs.reading || '')
          setIsEditingExistingRuby(true)
          activeRubyPosRef.current = { from: pos, to: pos + node.nodeSize }

          if (rect) {
            setPopoverPos({ top: rect.bottom + 6, left: rect.left })
          }
          setPopoverOpen(true)
          return true
        }
        return false
      },
      transformPastedHTML: (html) => {
        // Lọc sạch style, chỉ giữ lại text, bold, u, ruby, rt
        return sanitizeJapaneseHtml(html)
      },
      transformPastedText: (text) => {
        // Gộp các dòng bị ngắt dòng cứng không cần thiết từ PDF
        return text.replace(/([^\n])\n([^\n])/g, '$1 $2')
      },
    },
    onUpdate: ({ editor }) => {
      const rawHtml = editor.getHTML()
      const sanitized = sanitizeJapaneseHtml(rawHtml)
      onChange(sanitized)
    },
  })

  // Preload từ điển Kuroshiro ngay khi RichTextField khởi tạo
  useEffect(() => {
    import('./utils/kuroshiroService')
      .then(({ kuroshiroService }) => kuroshiroService.init())
      .catch(() => {})
  }, [])

  // Trigger mở popover từ đoạn text bôi đen
  const triggerFuriganaFromSelection = useCallback(() => {
    if (!editor || disabled) return

    const { from, to } = editor.state.selection
    const selectedText = editor.state.doc.textBetween(from, to, ' ').trim()

    if (!selectedText) return

    activeRubyPosRef.current = { from, to }
    setPopoverBase(selectedText)
    setPopoverReading('')
    setIsEditingExistingRuby(false)

    // Tính toán tọa độ popover cạnh selection
    try {
      const coords = editor.view.coordsAtPos(from)
      setPopoverPos({ top: coords.bottom + 8, left: Math.max(10, coords.left - 40) })
    } catch (_) {
      setPopoverPos(undefined)
    }

    setPopoverOpen(true)
  }, [editor, disabled])

  // Áp dụng Furigana từ popover vào editor
  const handleApplyFurigana = (reading: string) => {
    if (!editor || !activeRubyPosRef.current) return

    const { from, to } = activeRubyPosRef.current

    // Thay thế đoạn selection bằng Node ruby
    editor
      .chain()
      .focus()
      .insertContentAt(
        { from, to },
        {
          type: 'ruby',
          attrs: {
            base: popoverBase,
            reading: reading.trim(),
          },
        }
      )
      .run()

    setPopoverOpen(false)
  }

  // Xóa Furigana (giữ lại chữ Hán base)
  const handleRemoveFurigana = () => {
    if (!editor || !activeRubyPosRef.current) return

    const { from, to } = activeRubyPosRef.current
    editor
      .chain()
      .focus()
      .insertContentAt({ from, to }, popoverBase)
      .run()

    setPopoverOpen(false)
  }

  // Chèn nhãn người nói
  const handleInsertSpeaker = (label: string) => {
    if (!editor) return
    editor.chain().focus().insertSpeaker(label).run()
    setShowSpeakerMenu(false)
  }

  // Chèn số câu (1番, 2番...)
  const handleInsertQuestionNumber = () => {
    if (!editor) return
    const text = editor.getText()
    const matches = text.match(/(\d+)番/g)
    const nextNum = matches ? matches.length + 1 : 1
    editor.chain().focus().insertContent(`<b>${nextNum}番</b> `).run()
  }

  // Đồng bộ giá trị từ ngoài vào editor mà không làm mất con trỏ (tránh vòng lặp)
  useEffect(() => {
    if (!editor) return

    const currentHtml = editor.getHTML()
    const targetHtml = normalizeLegacy(value)

    if (currentHtml !== targetHtml && !editor.isFocused) {
      editor.commands.setContent(targetHtml, false)
    }
  }, [value, editor])

  useEffect(() => {
    if (editor) {
      editor.setEditable(!disabled)
    }
  }, [disabled, editor])

  if (!editor) {
    return null
  }

  const charCount = editor.storage.characterCount.characters()

  // --- SINGLELINE COMPACT VARIANT CHO ĐÁP ÁN ---
  if (isSingleLine) {
    return (
      <div className={`relative ${className}`}>
        <div className="relative flex items-center">
          <div
            className={`w-full border rounded-xl transition bg-white pr-24 ${
              disabled
                ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
                : 'border-slate-300 hover:border-indigo-400 focus-within:border-indigo-600 focus-within:ring-2 focus-within:ring-indigo-500/20 shadow-2xs'
            }`}
          >
            <EditorContent editor={editor} />
          </div>

          {/* Micro Toolbar đính kèm ngay mép phải ô input */}
          {!disabled && (
            <div className="absolute right-1.5 flex items-center gap-0.5 bg-slate-50/90 backdrop-blur-xs p-0.5 rounded-lg border border-slate-200 shadow-2xs">
              <button
                type="button"
                onClick={() => editor.chain().focus().toggleBold().run()}
                title="In đậm (Ctrl+B)"
                className={`w-5 h-5 flex items-center justify-center rounded font-bold transition cursor-pointer ${
                  editor.isActive('bold') ? 'bg-indigo-600 text-white' : 'hover:bg-slate-200 text-slate-700'
                }`}
              >
                <BoldIcon className="w-2.5 h-2.5" />
              </button>
              <button
                type="button"
                onClick={() => editor.chain().focus().toggleUnderline().run()}
                title="Gạch chân (Ctrl+U)"
                className={`w-5 h-5 flex items-center justify-center rounded font-bold transition cursor-pointer ${
                  editor.isActive('underline') ? 'bg-indigo-600 text-white' : 'hover:bg-slate-200 text-slate-700'
                }`}
              >
                <UnderlineIcon className="w-2.5 h-2.5" />
              </button>
              {enableFurigana && (
                <button
                  type="button"
                  onClick={triggerFuriganaFromSelection}
                  title="Thêm Furigana (Ctrl+Shift+R)"
                  className="px-1.5 h-5 flex items-center gap-0.5 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold transition cursor-pointer border border-indigo-200/80 text-[10px]"
                >
                  <Sparkles className="w-2.5 h-2.5 text-indigo-600" />
                  <span>ルビ</span>
                </button>
              )}
            </div>
          )}
        </div>

        {/* Popover Furigana */}
        <FuriganaPopover
          isOpen={popoverOpen}
          baseText={popoverBase}
          initialReading={popoverReading}
          position={popoverPos}
          onApply={handleApplyFurigana}
          onRemove={isEditingExistingRuby ? handleRemoveFurigana : undefined}
          onClose={() => setPopoverOpen(false)}
        />
      </div>
    )
  }

  // --- MULTILINE VARIANT CHO CÂU HỎI & SCRIPTS ---
  return (
    <div
      className={`border rounded-xl transition bg-white ${
        disabled
          ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
          : 'border-slate-300 hover:border-indigo-400 focus-within:border-indigo-600 focus-within:ring-2 focus-within:ring-indigo-500/20 shadow-2xs'
      } ${className}`}
    >
      {/* HEADER & TOOLBAR */}
      <div className="flex flex-wrap items-center justify-between gap-1.5 px-3 py-1.5 border-b border-slate-100 bg-slate-50/70 rounded-t-xl text-xs">
        {label ? (
          <span className="font-bold text-slate-700 flex items-center gap-1">
            {label}
          </span>
        ) : <div />}

        {!disabled && (
          <div className="flex flex-wrap items-center gap-1">
            {/* Bold */}
            <button
              type="button"
              onClick={() => editor.chain().focus().toggleBold().run()}
              title="In đậm (Ctrl+B)"
              className={`p-1 px-1.5 rounded transition font-bold flex items-center cursor-pointer ${
                editor.isActive('bold')
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'bg-white hover:bg-slate-200 text-slate-700 border border-slate-200'
              }`}
            >
              <BoldIcon className="w-3.5 h-3.5" />
            </button>

            {/* Underline */}
            <button
              type="button"
              onClick={() => editor.chain().focus().toggleUnderline().run()}
              title="Gạch chân (Ctrl+U)"
              className={`p-1 px-1.5 rounded transition font-bold flex items-center cursor-pointer ${
                editor.isActive('underline')
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'bg-white hover:bg-slate-200 text-slate-700 border border-slate-200'
              }`}
            >
              <UnderlineIcon className="w-3.5 h-3.5" />
            </button>

            {/* Furigana */}
            {enableFurigana && (
              <button
                type="button"
                onClick={triggerFuriganaFromSelection}
                title="Bôi đen chữ Hán và bấm để thêm Furigana (Ctrl+Shift+R)"
                className="p-1 px-2 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold transition flex items-center gap-1 cursor-pointer border border-indigo-200/80 shadow-2xs text-[11px]"
              >
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                <span>ルビ Furigana</span>
              </button>
            )}

            {/* Người nói (Chỉ bật cho Scripts) */}
            {enableSpeakerLabel && (
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowSpeakerMenu(!showSpeakerMenu)}
                  title="Chèn nhãn người nói (男/女/先生/学生)"
                  className="p-1 px-2 rounded bg-purple-50 hover:bg-purple-100 text-purple-700 font-bold transition flex items-center gap-1 cursor-pointer border border-purple-200 text-[11px]"
                >
                  <User className="w-3.5 h-3.5 text-purple-600" />
                  <span>Người nói</span>
                </button>

                {showSpeakerMenu && (
                  <div className="absolute top-full left-0 mt-1 z-30 bg-white border border-slate-200 rounded-xl shadow-lg p-1.5 flex gap-1 animate-in fade-in zoom-in-95 duration-100">
                    {['男', '女', '先生', '学生', '店員'].map((spk) => (
                      <button
                        key={spk}
                        type="button"
                        onClick={() => handleInsertSpeaker(spk)}
                        className="px-2 py-1 text-xs font-bold rounded-lg hover:bg-indigo-50 text-indigo-700 border border-transparent hover:border-indigo-200 transition cursor-pointer"
                      >
                        {spk}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Chèn số câu (1番, 2番...) */}
            {enableSpeakerLabel && (
              <button
                type="button"
                onClick={handleInsertQuestionNumber}
                title="Chèn số thứ tự câu (1番, 2番...)"
                className="p-1 px-1.5 rounded bg-white hover:bg-slate-200 text-slate-700 border border-slate-200 text-[11px] font-bold flex items-center gap-0.5 cursor-pointer"
              >
                <Hash className="w-3 h-3 text-slate-500" />
                <span>Số câu</span>
              </button>
            )}

            {/* Chèn Dấu sao JLPT */}
            {showStarButton && (
              <button
                type="button"
                onClick={() => editor.chain().focus().insertContent('＿＿＿　＿＿＿　＿★＿　＿＿＿').run()}
                title="Chèn mẫu câu hỏi sắp xếp sao ★"
                className="p-1 px-1.5 rounded bg-amber-50 hover:bg-amber-100 text-amber-700 font-bold transition flex items-center gap-0.5 cursor-pointer border border-amber-200 text-[11px]"
              >
                <Star className="w-3 h-3 fill-amber-500 text-amber-600" />
                <span>Dấu sao</span>
              </button>
            )}

            {/* Chèn Ô trống 【 】 */}
            {showClozeButton && (
              <button
                type="button"
                onClick={() => editor.chain().focus().insertContent('【 21 】').run()}
                title="Chèn ô trống dạng bài đục lỗ (【 21 】)"
                className="p-1 px-1.5 rounded bg-sky-50 hover:bg-sky-100 text-sky-700 font-bold transition flex items-center gap-0.5 cursor-pointer border border-sky-200 text-[11px]"
              >
                <Brackets className="w-3 h-3 text-sky-600" />
                <span>【 】</span>
              </button>
            )}

            {/* Undo */}
            <button
              type="button"
              onClick={() => editor.chain().focus().undo().run()}
              disabled={!editor.can().undo()}
              title="Hoàn tác (Ctrl+Z)"
              className="p-1 px-1.5 rounded bg-white hover:bg-slate-200 text-slate-600 border border-slate-200 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition"
            >
              <RotateCcw className="w-3.5 h-3.5" />
            </button>

            {/* Redo */}
            <button
              type="button"
              onClick={() => editor.chain().focus().redo().run()}
              disabled={!editor.can().redo()}
              title="Làm lại (Ctrl+Shift+Z)"
              className="p-1 px-1.5 rounded bg-white hover:bg-slate-200 text-slate-600 border border-slate-200 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition"
            >
              <RotateCw className="w-3.5 h-3.5" />
            </button>

            {/* Clear Formatting */}
            <button
              type="button"
              onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}
              title="Xóa định dạng"
              className="p-1 px-1.5 rounded bg-white hover:bg-slate-200 text-slate-600 border border-slate-200 cursor-pointer transition"
            >
              <RemoveFormatting className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>

      {/* VÙNG NHẬP WYSIWYG */}
      <div
        className="w-full overflow-y-auto"
        style={{ minHeight: `${minRows * 2.2}rem` }}
      >
        <EditorContent editor={editor} />
      </div>

      {/* FOOTER: BỘ ĐẾM KÝ TỰ & HELPER */}
      <div className="flex items-center justify-between px-3 py-1 bg-slate-50/50 border-t border-slate-100 text-[10px] text-slate-400 font-mono rounded-b-xl">
        <span>{helperText || ''}</span>
        <span>{charCount} ký tự</span>
      </div>

      {/* Popover Furigana */}
      <FuriganaPopover
        isOpen={popoverOpen}
        baseText={popoverBase}
        initialReading={popoverReading}
        position={popoverPos}
        onApply={handleApplyFurigana}
        onRemove={isEditingExistingRuby ? handleRemoveFurigana : undefined}
        onClose={() => setPopoverOpen(false)}
      />
    </div>
  )
}

export default RichTextField
