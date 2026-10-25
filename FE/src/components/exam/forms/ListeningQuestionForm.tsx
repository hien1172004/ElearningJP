import React, { useState } from 'react'
import { Image as ImageIcon, Volume2, Sparkles, Trash2 } from 'lucide-react'
import { QuestionAudioPlayer } from '@/components/exam/QuestionAudioPlayer'
import { AudioSegmentTrimmer } from '@/components/exam/AudioSegmentTrimmer'
import { FileUploadButton } from '@/components/exam/FileUploadButton'

export interface ListeningOptionItem {
  id?: number
  optionKey: number
  optionText: string
  optionImageUrl?: string
  isCorrect: boolean
}

export interface ListeningQuestionFormData {
  questionText: string
  audioUrl?: string
  audioStartTime?: number
  audioEndTime?: number
  imageUrl?: string
  hasVisualOptions?: boolean
  transcriptJp?: string
  transcriptVi?: string
  explanationVi?: string
  options: ListeningOptionItem[]
  points?: number
}

interface ListeningQuestionFormProps {
  data: ListeningQuestionFormData
  onChange: (updated: ListeningQuestionFormData) => void
  disabled?: boolean
  activeSectionAudioUrl?: string
  defaultOptionCount?: number // 3 cho Mondai 4, 5; 4 cho Mondai 1, 2, 3
  supportsVisuals?: boolean
}

export const ListeningQuestionForm: React.FC<ListeningQuestionFormProps> = ({
  data,
  onChange,
  disabled = false,
  activeSectionAudioUrl,
  defaultOptionCount = 4,
  supportsVisuals = false,
}) => {
  const [audioMode, setAudioMode] = useState<'INDIVIDUAL' | 'SECTION_TIMESTAMP'>(
    data.audioStartTime !== undefined || !data.audioUrl ? 'SECTION_TIMESTAMP' : 'INDIVIDUAL'
  )

  // Số lượng options (3 hoặc 4)
  const targetCount = defaultOptionCount || 4
  const options = data.options && data.options.length >= targetCount
    ? data.options.slice(0, targetCount)
    : Array.from({ length: targetCount }, (_, idx) => ({
        optionKey: idx + 1,
        optionText: data.options?.[idx]?.optionText || '',
        optionImageUrl: data.options?.[idx]?.optionImageUrl || '',
        isCorrect: idx === 0,
      }))

  const effectiveAudioUrl = audioMode === 'INDIVIDUAL' && data.audioUrl
    ? data.audioUrl
    : activeSectionAudioUrl

  const handleOptionChange = (index: number, text: string, imgUrl?: string) => {
    const next = [...options]
    next[index] = {
      ...next[index],
      optionText: text,
      optionImageUrl: imgUrl !== undefined ? imgUrl : next[index].optionImageUrl,
    }
    onChange({ ...data, options: next })
  }

  const handleSelectCorrect = (index: number) => {
    const next = options.map((opt, idx) => ({
      ...opt,
      isCorrect: idx === index,
    }))
    onChange({ ...data, options: next })
  }

  return (
    <div className="space-y-4">
      {/* KHỐI 1: CẤU HÌNH & TRÌNH PHÁT AUDIO SCRUBBER */}
      <div className="p-4 bg-slate-900 text-white rounded-3xl space-y-3 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="font-extrabold flex items-center gap-1.5 text-xs text-indigo-300">
            <Volume2 className="w-4 h-4 text-indigo-400" />
            <span>Âm thanh cho câu hỏi này (Audio Player & Scrubber)</span>
          </span>

          <div className="flex items-center gap-1 bg-slate-800 p-1 rounded-xl text-xs">
            <button
              type="button"
              disabled={disabled}
              onClick={() => setAudioMode('INDIVIDUAL')}
              className={`px-2.5 py-1 rounded-lg font-bold transition cursor-pointer ${
                audioMode === 'INDIVIDUAL'
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Audio MP3 riêng
            </button>
            <button
              type="button"
              disabled={disabled}
              onClick={() => setAudioMode('SECTION_TIMESTAMP')}
              className={`px-2.5 py-1 rounded-lg font-bold transition cursor-pointer ${
                audioMode === 'SECTION_TIMESTAMP'
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Đoạn cắt từ Audio tổng
            </button>
          </div>
        </div>

        {audioMode === 'INDIVIDUAL' ? (
          <div className="space-y-3">
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-[11px] font-bold text-slate-300">
                  File MP3 âm thanh riêng của câu hỏi này:
                </label>
                <FileUploadButton
                  accept="audio"
                  label="Tải Audio từ máy (.mp3)"
                  disabled={disabled}
                  onUploaded={(url) => onChange({ ...data, audioUrl: url })}
                />
              </div>
              <input
                type="text"
                disabled={disabled}
                value={data.audioUrl || ''}
                onChange={(e) => onChange({ ...data, audioUrl: e.target.value })}
                placeholder="Hoặc dán URL: https://.../mondai1_cau1.mp3"
                className="w-full px-3 py-2 text-xs border border-slate-700 rounded-xl bg-slate-800 text-white font-mono focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
              />
            </div>

            {effectiveAudioUrl ? (
              <div className="pt-1">
                <QuestionAudioPlayer
                  audioUrl={effectiveAudioUrl}
                  title="Phát thử âm thanh câu hỏi (Audio riêng)"
                />
              </div>
            ) : (
              <p className="text-[11px] text-amber-400/90 italic pt-1">
                Chưa có Audio riêng. Bạn có thể bấm nút "Tải Audio từ máy" ở trên hoặc dán URL.
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            <AudioSegmentTrimmer
              audioUrl={data.audioUrl}
              masterAudioUrl={activeSectionAudioUrl}
              startTime={data.audioStartTime ?? 0}
              endTime={data.audioEndTime ?? 30}
              onChangeStartTime={(val) => onChange({ ...data, audioStartTime: val })}
              onChangeEndTime={(val) => onChange({ ...data, audioEndTime: val })}
              title="Kéo chỉnh đoạn âm thanh câu hỏi từ Audio Section"
            />

            {/* Nhập số chính xác thủ công nếu cần */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <div>
                <label className="block text-[10px] text-slate-400 mb-1">Bắt đầu từ giây thứ (Start):</label>
                <input
                  type="number"
                  min={0}
                  step={0.1}
                  disabled={disabled}
                  value={data.audioStartTime !== undefined ? data.audioStartTime : 0}
                  onChange={(e) => onChange({ ...data, audioStartTime: Number(e.target.value) })}
                  className="w-full px-3 py-1.5 text-xs border border-slate-700 rounded-xl bg-slate-800 text-white font-mono"
                />
              </div>
              <div>
                <label className="block text-[10px] text-slate-400 mb-1">Kết thúc ở giây thứ (End):</label>
                <input
                  type="number"
                  min={0}
                  step={0.1}
                  disabled={disabled}
                  value={data.audioEndTime !== undefined ? data.audioEndTime : 30}
                  onChange={(e) => onChange({ ...data, audioEndTime: Number(e.target.value) })}
                  className="w-full px-3 py-1.5 text-xs border border-slate-700 rounded-xl bg-slate-800 text-white font-mono"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* KHỐI 2: TRANH MINH HỌA TÌNH HUỐNG & ĐÁP ÁN TRANH */}
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-3xl space-y-3">
        <span className="font-extrabold text-slate-900 flex items-center gap-1.5 text-xs">
          <ImageIcon className="w-4 h-4 text-indigo-600" />
          <span>Hình ảnh tình huống & Lựa chọn đáp án bằng tranh</span>
        </span>

        {/* Tranh tình huống câu hỏi */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-[11px] font-bold text-slate-700">
              Tranh vẽ tình huống (Dành cho câu có tranh chung, hoặc Mondai 4 mũi tên ➡):
            </label>
            <FileUploadButton
              accept="image"
              label="Tải ảnh từ máy"
              disabled={disabled}
              onUploaded={(url) => onChange({ ...data, imageUrl: url })}
            />
          </div>
          <input
            type="text"
            disabled={disabled}
            value={data.imageUrl || ''}
            onChange={(e) => onChange({ ...data, imageUrl: e.target.value })}
            placeholder="Hoặc dán link URL: https://.../tranh_tinh_huong.png"
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-mono"
          />
          {data.imageUrl && (
            <div className="mt-2 p-2 border border-slate-200 rounded-xl bg-white max-w-xs relative group">
              <img src={data.imageUrl} alt="Tình huống" className="rounded-lg max-h-36 object-contain mx-auto" />
              <button
                type="button"
                onClick={() => onChange({ ...data, imageUrl: '' })}
                className="absolute top-3 right-3 p-1 rounded-lg bg-rose-600 text-white opacity-0 group-hover:opacity-100 transition shadow-xs"
                title="Xóa ảnh"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          )}
        </div>

        {/* Toggle 4 đáp án bằng tranh ảnh */}
        {supportsVisuals && (
          <div className="pt-2 border-t border-slate-200 flex items-center justify-between">
            <div className="space-y-0.5">
              <span className="text-xs font-bold text-slate-800 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-600" />
                <span>Các đáp án là tranh vẽ minh họa (Visual Options):</span>
              </span>
              <p className="text-[11px] text-slate-500">
                Bật tùy chọn này nếu đề thi có 4 tranh 1, 2, 3, 4. Khi bật, bạn chỉ cần tải ảnh 4 tranh mà không cần phải nhập chữ.
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                disabled={disabled}
                checked={data.hasVisualOptions || false}
                onChange={(e) => onChange({ ...data, hasVisualOptions: e.target.checked })}
                className="sr-only peer"
              />
              <div className="w-9 h-5 bg-slate-300 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-600"></div>
            </label>
          </div>
        )}
      </div>

      {/* KHỐI 3: NỘI DUNG CÂU HỎI & CÁC LỰA CHỌN */}
      <div className="space-y-3">
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <label className="block text-xs font-bold text-slate-800">
              Nội dung câu hỏi (Không bắt buộc nếu là câu hỏi nghe tranh):
            </label>
            <button
              type="button"
              disabled={disabled}
              onClick={() => onChange({ ...data, questionText: '1番' })}
              className="text-[10px] text-indigo-600 hover:underline font-bold"
            >
              Gợi ý điền "1番"
            </button>
          </div>
          <input
            type="text"
            disabled={disabled}
            value={data.questionText || ''}
            onChange={(e) => onChange({ ...data, questionText: e.target.value })}
            placeholder="Ví dụ: 1番 hoặc 男の人はこのあと何をしますか (Để trống sẽ tự động lấy số câu)"
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-medium focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
          />
        </div>

        {/* CÁC PHƯƠNG ÁN LỰA CHỌN */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-800">
            {options.length} Phương án lựa chọn (Chọn 1 đáp án đúng): *
          </label>

          {data.hasVisualOptions ? (
            /* GIAO DIỆN CHUYÊN BIỆT CHO ĐÁP ÁN LÀ TRANH VẼ (VISUAL OPTIONS) */
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {options.map((opt, idx) => {
                const isCorrect = opt.isCorrect
                return (
                  <div
                    key={opt.optionKey}
                    className={`p-3.5 rounded-2xl border-2 transition-all flex flex-col justify-between gap-3 ${
                      isCorrect
                        ? 'bg-emerald-50/70 border-emerald-500 shadow-sm ring-1 ring-emerald-300'
                        : 'bg-white border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span
                          className={`w-7 h-7 rounded-xl flex items-center justify-center font-black text-xs ${
                            isCorrect ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {opt.optionKey}
                        </span>
                        <span className="font-bold text-xs text-slate-800">Tranh {opt.optionKey}</span>
                      </div>

                      <label
                        onClick={() => !disabled && handleSelectCorrect(idx)}
                        className={`flex items-center gap-1.5 text-xs font-bold cursor-pointer px-2.5 py-1 rounded-xl transition ${
                          isCorrect
                            ? 'text-emerald-700 bg-emerald-100 font-black'
                            : 'text-slate-400 hover:text-slate-600'
                        }`}
                      >
                        <input
                          type="radio"
                          name="listeningCorrectVisualOption"
                          disabled={disabled}
                          checked={isCorrect}
                          onChange={() => handleSelectCorrect(idx)}
                          className="w-4 h-4 text-emerald-600 accent-emerald-600 cursor-pointer"
                        />
                        <span>{isCorrect ? 'Đáp án đúng' : 'Chọn đúng'}</span>
                      </label>
                    </div>

                    {/* Vùng xem trước & Tải ảnh cho tranh này */}
                    {opt.optionImageUrl ? (
                      <div className="relative group bg-slate-50 border border-slate-200 rounded-xl p-2 flex flex-col items-center">
                        <img
                          src={opt.optionImageUrl}
                          alt={`Tranh ${opt.optionKey}`}
                          className="h-32 object-contain rounded-lg"
                        />
                        <button
                          type="button"
                          onClick={() => handleOptionChange(idx, '', '')}
                          className="absolute top-2 right-2 p-1.5 rounded-lg bg-rose-600 text-white opacity-0 group-hover:opacity-100 transition shadow-xs cursor-pointer"
                          title="Xóa tranh này"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ) : (
                      <div className="border-2 border-dashed border-slate-200 rounded-xl p-4 flex flex-col items-center justify-center text-center bg-slate-50/50 min-h-[120px] gap-2">
                        <ImageIcon className="w-7 h-7 text-slate-400" />
                        <span className="text-[11px] text-slate-500 font-medium">Chưa có ảnh cho Tranh {opt.optionKey}</span>
                        <FileUploadButton
                          accept="image"
                          label={`Tải ảnh Tranh ${opt.optionKey} từ máy`}
                          disabled={disabled}
                          onUploaded={(url) => handleOptionChange(idx, '', url)}
                        />
                      </div>
                    )}

                    {/* Ô nhập URL dự phòng */}
                    <div className="flex items-center gap-2 pt-1 border-t border-slate-100">
                      <input
                        type="text"
                        disabled={disabled}
                        value={opt.optionImageUrl || ''}
                        onChange={(e) => handleOptionChange(idx, '', e.target.value)}
                        placeholder={`Hoặc dán URL ảnh Tranh ${opt.optionKey}...`}
                        className="w-full px-2.5 py-1 text-[11px] border border-slate-200 rounded-lg bg-white font-mono"
                      />
                      {!opt.optionImageUrl && (
                        <FileUploadButton
                          accept="image"
                          label="Tải"
                          disabled={disabled}
                          onUploaded={(url) => handleOptionChange(idx, '', url)}
                        />
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          ) : (
            /* GIAO DIỆN CHỌN ĐÁP ÁN BẰNG CHỮ THƯỜNG */
            <div className="space-y-2">
              {options.map((opt, idx) => {
                const isCorrect = opt.isCorrect
                return (
                  <div
                    key={opt.optionKey}
                    className={`p-3 rounded-2xl border transition flex flex-col gap-2 ${
                      isCorrect
                        ? 'bg-emerald-50/70 border-emerald-300 ring-1 ring-emerald-200'
                        : 'bg-white border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center gap-3">
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
                          name="listeningCorrectOption"
                          disabled={disabled}
                          checked={isCorrect}
                          onChange={() => handleSelectCorrect(idx)}
                          className="w-4 h-4 text-emerald-600 accent-emerald-600 cursor-pointer"
                        />
                        <span>{isCorrect ? 'Đáp án đúng' : 'Chọn đúng'}</span>
                      </label>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* Transcripts tiếng Nhật và tiếng Việt */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              Transcript tiếng Nhật (Lời thoại hội thoại):
            </label>
            <textarea
              rows={3}
              disabled={disabled}
              value={data.transcriptJp || ''}
              onChange={(e) => onChange({ ...data, transcriptJp: e.target.value })}
              placeholder="男：明日の会議は何時からですか。&#10;女：10時からですよ。"
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white font-serif focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              Bản dịch tiếng Việt của lời thoại:
            </label>
            <textarea
              rows={3}
              disabled={disabled}
              value={data.transcriptVi || ''}
              onChange={(e) => onChange({ ...data, transcriptVi: e.target.value })}
              placeholder="Nam: Cuộc họp ngày mai mấy giờ bắt đầu?&#10;Nữ: 10 giờ đấy."
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
            />
          </div>
        </div>

        {/* Lời giải thích */}
        <div className="space-y-1">
          <label className="block text-xs font-bold text-slate-700">
            Giải thích đáp án (Tiếng Việt):
          </label>
          <textarea
            rows={2}
            disabled={disabled}
            value={data.explanationVi || ''}
            onChange={(e) => onChange({ ...data, explanationVi: e.target.value })}
            placeholder="Người nam hỏi giờ họp, người nữ trả lời là từ 10 giờ nên chọn đáp án..."
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
          />
        </div>
      </div>
    </div>
  )
}
