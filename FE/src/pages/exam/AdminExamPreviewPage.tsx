import React, { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { examService } from '../../services/exam.service'
import type {
  AdminExamPreview,
  AdminTakingQuestion,
  AdminTakingOption,
  AdminTakingSection,
  UpdateQuestionInlinePayload,
} from '../../types/exam.types'
import {
  ArrowLeft,
  Check,
  Edit3,
  Save,
  Volume2,
  Play,
  Pause,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  FileCheck,
} from 'lucide-react'

export const AdminExamPreviewPage: React.FC = () => {
  const { examId } = useParams<{ examId: string }>()
  const navigate = useNavigate()

  const [previewData, setPreviewData] = useState<AdminExamPreview | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState<boolean>(false)
  const [publishSuccess, setPublishSuccess] = useState<boolean>(false)

  // Active section tab
  const [activeSectionIdx, setActiveSectionIdx] = useState<number>(0)

  // Question editing state: questionId -> draft edit object
  const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<UpdateQuestionInlinePayload>({
    questionText: '',
  })
  const [savingQuestion, setSavingQuestion] = useState<boolean>(false)

  // Audio testing
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const [isPlayingAudio, setIsPlayingAudio] = useState<boolean>(false)
  const timerRef = useRef<any>(null)

  useEffect(() => {
    if (!examId) return
    const fetchPreview = async () => {
      try {
        setLoading(true)
        const data = await examService.getAdminExamPreview(Number(examId))
        setPreviewData(data)
      } catch (err: any) {
        setError(err.response?.data?.message || 'Không thể tải đề thi để xem trước')
      } finally {
        setLoading(false)
      }
    }
    fetchPreview()

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [examId])

  const handleStartEdit = (q: AdminTakingQuestion) => {
    setEditingQuestionId(q.id)
    setEditForm({
      questionText: q.questionText,
      audioStartTime: q.audioStartTime,
      audioEndTime: q.audioEndTime,
      transcriptJp: q.transcriptJp,
      transcriptVi: q.transcriptVi,
      explanationVi: q.explanationVi,
      starPosition: q.starPosition,
      imageUrl: q.imageUrl,
      options: q.options.map((opt: AdminTakingOption) => ({
        id: opt.id,
        optionKey: opt.optionKey,
        optionText: opt.optionText || '',
        optionImageUrl: opt.optionImageUrl,
        correct: opt.correct,
      })),
    })
  }

  const handleSaveQuestion = async (qId: number) => {
    if (!examId) return
    try {
      setSavingQuestion(true)
      const updated = await examService.updateQuestionInline(Number(examId), qId, editForm)

      // Cập nhật state local
      if (previewData) {
        const updatedSections = previewData.sections.map((sec: AdminTakingSection) => ({
          ...sec,
          questions: sec.questions.map((q: AdminTakingQuestion) => (q.id === qId ? { ...q, ...updated } : q)),
        }))
        setPreviewData({ ...previewData, sections: updatedSections })
      }
      setEditingQuestionId(null)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi lưu thay đổi câu hỏi')
    } finally {
      setSavingQuestion(false)
    }
  }

  const handleTestAudio = (audioUrl?: string, start?: number, end?: number) => {
    if (!audioUrl || start === undefined) return

    if (isPlayingAudio) {
      if (audioRef.current) audioRef.current.pause()
      setIsPlayingAudio(false)
      if (timerRef.current) clearInterval(timerRef.current)
      return
    }

    if (!audioRef.current) {
      audioRef.current = new Audio(audioUrl)
    } else if (audioRef.current.src !== audioUrl) {
      audioRef.current.src = audioUrl
    }

    const audio = audioRef.current
    audio.currentTime = start
    audio.play()
    setIsPlayingAudio(true)

    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = setInterval(() => {
      if (end !== undefined && audio.currentTime >= end) {
        audio.pause()
        setIsPlayingAudio(false)
        clearInterval(timerRef.current)
      }
    }, 200)

    audio.onended = () => {
      setIsPlayingAudio(false)
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }

  const handlePublishExam = async () => {
    if (!examId) return
    if (!window.confirm('Bạn có chắc chắn muốn phát hành đề thi này cho tất cả học viên không?')) return

    try {
      setPublishing(true)
      await examService.publishExam(Number(examId))
      setPublishSuccess(true)
      setTimeout(() => {
        navigate('/exams')
      }, 1500)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi phát hành đề thi')
    } finally {
      setPublishing(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 dark:text-slate-400 font-medium">Đang tải đề thi để biên tập...</p>
        </div>
      </div>
    )
  }

  if (error || !previewData) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-800 rounded-2xl p-8 max-w-md w-full text-center border border-slate-200">
          <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-3" />
          <h2 className="text-xl font-bold text-slate-800 dark:text-white mb-2">Lỗi tải đề thi</h2>
          <p className="text-slate-500 text-sm mb-6">{error || 'Không tìm thấy thông tin đề thi.'}</p>
          <button
            onClick={() => navigate('/exams')}
            className="w-full py-2.5 bg-indigo-600 text-white rounded-xl font-medium"
          >
            Quay lại sảnh đề thi
          </button>
        </div>
      </div>
    )
  }

  const activeSection = previewData.sections[activeSectionIdx] || previewData.sections[0]

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-slate-950 flex flex-col font-sans">
      {/* Top action header */}
      <header className="sticky top-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur border-b border-slate-200 dark:border-slate-800 px-4 lg:px-8 py-3.5 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/exams')}
            className="p-2 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 transition"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 rounded text-xs font-black">
                {previewData.published ? 'ĐÃ PHÁT HÀNH' : 'BẢN NHÁP (DRAFT)'}
              </span>
              <span className="px-2 py-0.5 bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 rounded text-xs font-bold">
                {previewData.jlptLevel} • {previewData.totalQuestions} câu hỏi
              </span>
            </div>
            <h1 className="text-base md:text-lg font-bold text-slate-900 dark:text-white line-clamp-1">
              {previewData.title}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {publishSuccess ? (
            <div className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-sm shadow">
              <Check className="w-4 h-4" />
              Đã phát hành thành công!
            </div>
          ) : (
            <button
              onClick={handlePublishExam}
              disabled={publishing || previewData.published}
              className={`inline-flex items-center gap-2 px-6 py-2.5 rounded-xl font-bold text-sm text-white shadow-lg transition ${
                previewData.published
                  ? 'bg-slate-400 cursor-not-allowed'
                  : 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/30'
              }`}
            >
              <FileCheck className="w-4 h-4" />
              <span>{publishing ? 'Đang phát hành...' : previewData.published ? 'Đã phát hành' : 'Phát hành đề thi'}</span>
            </button>
          )}
        </div>
      </header>

      {/* Sections Tab */}
      {previewData.sections.length > 1 && (
        <div className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-4 lg:px-8 py-2">
          <div className="flex gap-2 overflow-x-auto max-w-6xl mx-auto">
            {previewData.sections.map((sec: AdminTakingSection, idx: number) => (
              <button
                key={sec.id}
                onClick={() => {
                  setActiveSectionIdx(idx)
                  setEditingQuestionId(null)
                }}
                className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
                  activeSectionIdx === idx
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200'
                }`}
              >
                Ca {idx + 1}: {sec.sectionTitle} ({sec.questions.length} câu)
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Paper-Style Questions Stream */}
      <div className="max-w-4xl w-full mx-auto p-4 lg:p-6 space-y-6 flex-1">
        <div className="p-4 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-900/60 rounded-2xl flex items-start gap-3">
          <Sparkles className="w-5 h-5 text-indigo-600 flex-shrink-0 mt-0.5" />
          <div className="text-xs text-indigo-900 dark:text-indigo-200">
            <strong>Chế độ Xem trước & Sửa trực tiếp (WYSIWYG Inline Editor):</strong> Giáo viên có thể bấm "Chỉnh sửa câu này" để cập nhật nội dung văn bản, mốc giây audio, nghe thử tức thì, chỉnh đáp án đúng hoặc bổ sung giải thích trước khi bấm Phát hành.
          </div>
        </div>

        {activeSection?.questions.map((q: AdminTakingQuestion) => {
          const isEditing = editingQuestionId === q.id

          return (
            <div
              key={q.id}
              className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200/90 dark:border-slate-800 p-6 relative transition"
            >
              {/* Question Header */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2.5">
                  <span className="w-8 h-8 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-white font-extrabold flex items-center justify-center text-sm border border-slate-200 dark:border-slate-700">
                    {q.questionNumber}
                  </span>
                  {q.mondaiNameVi && (
                    <span className="text-xs font-semibold px-2.5 py-1 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-lg">
                      {q.mondaiNameVi}
                    </span>
                  )}
                  {q.starPosition && (
                    <span className="text-xs font-bold px-2 py-0.5 bg-amber-100 text-amber-800 rounded">
                      ★ Vị trí sao: {q.starPosition}
                    </span>
                  )}
                </div>

                {!isEditing ? (
                  <button
                    onClick={() => handleStartEdit(q)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-bold transition"
                  >
                    <Edit3 className="w-3.5 h-3.5 text-indigo-600" />
                    Chỉnh sửa câu này
                  </button>
                ) : (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setEditingQuestionId(null)}
                      className="px-3 py-1.5 rounded-xl border border-slate-300 text-slate-600 text-xs font-semibold hover:bg-slate-50"
                    >
                      Hủy
                    </button>
                    <button
                      onClick={() => handleSaveQuestion(q.id)}
                      disabled={savingQuestion}
                      className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow hover:bg-indigo-700 transition"
                    >
                      <Save className="w-3.5 h-3.5" />
                      {savingQuestion ? 'Đang lưu...' : 'Lưu câu này'}
                    </button>
                  </div>
                )}
              </div>

              {/* Edit Mode vs Preview Mode */}
              {isEditing ? (
                <div className="space-y-4 pt-2 border-t border-slate-100 dark:border-slate-800">
                  {/* Text Question */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                      Nội dung câu hỏi:
                    </label>
                    <textarea
                      rows={3}
                      value={editForm.questionText}
                      onChange={(e) => setEditForm({ ...editForm, questionText: e.target.value })}
                      className="w-full p-3 border border-slate-300 dark:border-slate-700 rounded-xl text-sm bg-slate-50 dark:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-sans"
                    />
                  </div>

                  {/* Audio Controls (If listening session) */}
                  {activeSection.audioUrl && (
                    <div className="p-3.5 bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-200 dark:border-indigo-900/40 rounded-xl space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-indigo-900 dark:text-indigo-300 flex items-center gap-1">
                          <Volume2 className="w-4 h-4" />
                          Mốc thời gian Audio (giây):
                        </span>
                        <button
                          type="button"
                          onClick={() =>
                            handleTestAudio(
                              activeSection.audioUrl,
                              editForm.audioStartTime,
                              editForm.audioEndTime
                            )
                          }
                          className="inline-flex items-center gap-1.5 px-3 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
                        >
                          {isPlayingAudio ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                          Nghe thử đoạn này
                        </button>
                      </div>

                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs text-slate-500 mb-1">Bắt đầu (giây):</label>
                          <input
                            type="number"
                            step="0.1"
                            value={editForm.audioStartTime ?? ''}
                            onChange={(e) =>
                              setEditForm({
                                ...editForm,
                                audioStartTime: parseFloat(e.target.value) || 0,
                              })
                            }
                            className="w-full p-2 border border-slate-300 rounded-lg text-sm bg-white dark:bg-slate-800"
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-slate-500 mb-1">Kết thúc (giây):</label>
                          <input
                            type="number"
                            step="0.1"
                            value={editForm.audioEndTime ?? ''}
                            onChange={(e) =>
                              setEditForm({
                                ...editForm,
                                audioEndTime: parseFloat(e.target.value) || 0,
                              })
                            }
                            className="w-full p-2 border border-slate-300 rounded-lg text-sm bg-white dark:bg-slate-800"
                          />
                        </div>
                      </div>

                      {/* Transcript inputs */}
                      <div className="space-y-2 pt-2">
                        <div>
                          <label className="block text-xs font-semibold text-slate-600 mb-1">
                            Transcript tiếng Nhật (Scripts):
                          </label>
                          <textarea
                            rows={2}
                            value={editForm.transcriptJp || ''}
                            onChange={(e) =>
                              setEditForm({ ...editForm, transcriptJp: e.target.value })
                            }
                            className="w-full p-2.5 border border-slate-300 rounded-lg text-xs bg-white dark:bg-slate-800"
                          />
                        </div>
                        <div>
                          <label className="block text-xs font-semibold text-slate-600 mb-1">
                            Bản dịch tiếng Việt:
                          </label>
                          <textarea
                            rows={2}
                            value={editForm.transcriptVi || ''}
                            onChange={(e) =>
                              setEditForm({ ...editForm, transcriptVi: e.target.value })
                            }
                            className="w-full p-2.5 border border-slate-300 rounded-lg text-xs bg-white dark:bg-slate-800"
                          />
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Options edit */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-2">
                      Lựa chọn đáp án (Chọn Radio để đánh dấu đáp án đúng):
                    </label>
                    <div className="space-y-2.5">
                      {editForm.options?.map((opt: AdminTakingOption, idx: number) => (
                        <div
                          key={opt.id || idx}
                          className={`p-3 rounded-xl border flex items-center gap-3 ${
                            opt.correct
                              ? 'border-emerald-500 bg-emerald-50/50 dark:bg-emerald-950/20'
                              : 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800'
                          }`}
                        >
                          <input
                            type="radio"
                            name={`correct-opt-${q.id}`}
                            checked={opt.correct}
                            onChange={() => {
                              const updated = editForm.options?.map((o: AdminTakingOption, i: number) => ({
                                ...o,
                                correct: i === idx,
                              }))
                              setEditForm({ ...editForm, options: updated })
                            }}
                            className="w-4 h-4 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
                          />
                          <span className="w-6 h-6 rounded-lg bg-slate-100 dark:bg-slate-700 flex items-center justify-center text-xs font-bold text-slate-700 dark:text-slate-200">
                            {opt.optionKey}
                          </span>
                          <input
                            type="text"
                            value={opt.optionText}
                            onChange={(e) => {
                              const updated = [...(editForm.options || [])]
                              updated[idx].optionText = e.target.value
                              setEditForm({ ...editForm, options: updated })
                            }}
                            className="flex-1 p-2 border border-slate-300 dark:border-slate-600 rounded-lg text-sm bg-white dark:bg-slate-900"
                            placeholder={`Lựa chọn ${opt.optionKey}`}
                          />
                          {opt.correct && (
                            <span className="text-xs font-bold text-emerald-600 flex items-center gap-1">
                              <CheckCircle2 className="w-4 h-4" />
                              Đáp án đúng
                            </span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Explanation edit */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                      Lời giải thích chi tiết:
                    </label>
                    <textarea
                      rows={2}
                      value={editForm.explanationVi || ''}
                      onChange={(e) => setEditForm({ ...editForm, explanationVi: e.target.value })}
                      placeholder="Nhập giải thích ngữ pháp, từ vựng hoặc lý do chọn đáp án..."
                      className="w-full p-2.5 border border-slate-300 rounded-xl text-xs bg-slate-50 dark:bg-slate-800"
                    />
                  </div>
                </div>
              ) : (
                /* Read-Only Preview Mode */
                <div className="space-y-4">
                  {/* Passage */}
                  {q.passage && (
                    <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700 text-xs">
                      <div className="font-bold text-indigo-600 mb-1">Bài đọc: {q.passage.title}</div>
                      {q.passage.contentJp && <div className="text-slate-700">{q.passage.contentJp}</div>}
                      {q.passage.imageUrl && (
                        <img
                          src={q.passage.imageUrl}
                          alt="Poster"
                          className="max-h-60 rounded-lg mt-2 border"
                        />
                      )}
                    </div>
                  )}

                  {/* Audio badge */}
                  {q.audioStartTime !== undefined && (
                    <div className="flex items-center gap-3 text-xs text-indigo-700 bg-indigo-50 dark:bg-indigo-950/40 p-2.5 rounded-xl border border-indigo-200">
                      <Volume2 className="w-4 h-4 text-indigo-600" />
                      <span>
                        Mốc nghe: <strong>{q.audioStartTime}s - {q.audioEndTime}s</strong>
                      </span>
                      {activeSection.audioUrl && (
                        <button
                          type="button"
                          onClick={() =>
                            handleTestAudio(
                              activeSection.audioUrl,
                              q.audioStartTime,
                              q.audioEndTime
                            )
                          }
                          className="ml-auto inline-flex items-center gap-1 px-2.5 py-1 bg-indigo-600 text-white rounded-md text-xs font-semibold"
                        >
                          <Play className="w-3 h-3" />
                          Nghe thử
                        </button>
                      )}
                    </div>
                  )}

                  {/* Question Text */}
                  <div 
                    className="text-base font-semibold text-slate-900 dark:text-white [&_u]:underline [&_u]:underline-offset-4 [&_u]:decoration-2 [&_u]:font-bold"
                    dangerouslySetInnerHTML={{ __html: q.questionText }}
                  />

                  {/* Options */}
                  <div className="grid gap-2">
                    {q.options.map((opt: AdminTakingOption) => (
                      <div
                        key={opt.id}
                        className={`p-3 rounded-xl border flex items-center justify-between text-sm ${
                          opt.correct
                            ? 'border-emerald-500 bg-emerald-50/60 font-semibold text-emerald-900'
                            : 'border-slate-200 bg-white text-slate-700'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span
                            className={`w-6 h-6 rounded-md flex items-center justify-center text-xs font-bold ${
                              opt.correct ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {opt.optionKey}
                          </span>
                          <span>{opt.optionText}</span>
                        </div>
                        {opt.correct && (
                          <span className="text-xs text-emerald-600 font-bold flex items-center gap-1">
                            <Check className="w-4 h-4" />
                            Đáp án đúng
                          </span>
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Explanation */}
                  {q.explanationVi && (
                    <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-xl text-xs text-slate-600 dark:text-slate-400 border border-slate-200">
                      <strong>Giải thích:</strong> {q.explanationVi}
                    </div>
                  )}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
export default AdminExamPreviewPage
