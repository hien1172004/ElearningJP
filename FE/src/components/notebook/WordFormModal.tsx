import React, { useEffect, useState, useRef } from 'react'
import { Modal, Form, Input, Select, Button, Spin } from 'antd'
import { LoadingOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { dictionaryService } from '@/services/dictionary.service'
import type { NotebookWord, CreateNotebookWordRequest, UpdateNotebookWordRequest } from '@/types'

interface WordFormModalProps {
  open: boolean
  editingWord?: NotebookWord | null
  submitting?: boolean
  onCancel: () => void
  onSubmit: (values: CreateNotebookWordRequest | UpdateNotebookWordRequest) => void
}

const ALLOWED_POS = ['Từ vựng', 'Chữ Hán', 'Ngữ pháp'] as const

export const WordFormModal: React.FC<WordFormModalProps> = ({
  open,
  editingWord,
  submitting = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm()
  const [isAutoFilling, setIsAutoFilling] = useState(false)
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const userModifiedRef = useRef<{
    reading: boolean
    sinoVietnamese: boolean
    meaning: boolean
    partOfSpeech: boolean
  }>({
    reading: false,
    sinoVietnamese: false,
    meaning: false,
    partOfSpeech: false,
  })

  useEffect(() => {
    if (open) {
      if (editingWord) {
        userModifiedRef.current = {
          reading: true,
          sinoVietnamese: true,
          meaning: true,
          partOfSpeech: true,
        }
        const pos = ALLOWED_POS.includes(editingWord.partOfSpeech as any)
          ? editingWord.partOfSpeech
          : 'Từ vựng'

        form.setFieldsValue({
          word: editingWord.word,
          reading: editingWord.reading,
          meaning: editingWord.meaning,
          partOfSpeech: pos,
          userNote: editingWord.userNote,
          sinoVietnamese: editingWord.sinoVietnamese || editingWord.hanViet,
        })
      } else {
        userModifiedRef.current = {
          reading: false,
          sinoVietnamese: false,
          meaning: false,
          partOfSpeech: false,
        }
        form.resetFields()
        form.setFieldsValue({
          partOfSpeech: 'Từ vựng',
        })
      }
    }
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, [open, editingWord, form])

  // Tự động phân tích và điền Phiên âm + Âm Hán Việt theo chữ người dùng nhập
  const triggerAutoFill = async (
    rawWord: string,
    isManual = false,
    targetField?: 'reading' | 'sinoVietnamese'
  ) => {
    const trimmed = rawWord.trim()
    if (!trimmed) {
      if (!editingWord || isManual) {
        userModifiedRef.current = {
          reading: false,
          sinoVietnamese: false,
          meaning: false,
          partOfSpeech: false,
        }
        form.setFieldsValue({
          reading: '',
          sinoVietnamese: '',
          meaning: '',
          partOfSpeech: 'Từ vựng',
        })
      }
      return
    }

    // Nếu đang chỉnh sửa từ có sẵn và không phải người dùng chủ động bấm nút "Tự động điền", không ghi đè nếu từ chưa đổi
    if (editingWord && !isManual && !targetField && trimmed === editingWord.word?.trim()) {
      return
    }

    if (isManual) {
      userModifiedRef.current = {
        reading: false,
        sinoVietnamese: false,
        meaning: false,
        partOfSpeech: false,
      }
    } else if (targetField) {
      userModifiedRef.current[targetField] = false
    }

    setIsAutoFilling(true)
    try {
      let suggestedPartOfSpeech = 'Từ vựng'
      // 1. Nếu là 1 chữ Hán đơn lẻ -> gợi ý từ loại "Chữ Hán"
      if (/^[\u4e00-\u9faf]$/.test(trimmed)) {
        suggestedPartOfSpeech = 'Chữ Hán'
      }

      // 2. Nếu là từ thuần Kana -> Phiên âm chính là từ đó, không có Hán Việt
      const isPureKana = /^[\u3040-\u309f\u30a0-\u30ff\s]+$/.test(trimmed)
      let finalReading = isPureKana ? trimmed : ''
      let finalHanViet = ''
      let suggestedMeaning = ''

      // 3. Tra cứu từ điển hệ thống
      const dictRes = await dictionaryService.searchUnified(trimmed).catch(() => null)

      if (dictRes?.data) {
        const vocabs = dictRes.data.vocabularies || []
        const kanjis = dictRes.data.kanjis || []
        const grammars = dictRes.data.grammars || []

        // Tìm từ vựng khớp chính xác (hoặc khớp hiragana nếu là từ thuần Kana)
        const exactVocab =
          vocabs.find((v) => v.word === trimmed) ||
          (isPureKana ? vocabs.find((v) => v.hiragana === trimmed) : undefined)
        if (exactVocab) {
          if (exactVocab.hiragana) finalReading = exactVocab.hiragana
          if (exactVocab.hanViet) finalHanViet = exactVocab.hanViet
          if (exactVocab.meaningVi) suggestedMeaning = exactVocab.meaningVi
        }

        // Tự động nhận diện ngữ pháp
        if (grammars.length > 0 && grammars.some((g) => g.title === trimmed)) {
          suggestedPartOfSpeech = 'Ngữ pháp'
          if (!suggestedMeaning && grammars[0]?.meaningVi) {
            suggestedMeaning = grammars[0].meaningVi
          }
        }

        // Nếu là chữ Hán đơn lẻ mà chưa có reading, lấy từ kunyomi / onyomi
        if (!finalReading && kanjis.length > 0) {
          const k = kanjis.find((item) => item.character === trimmed)
          if (k) {
            const rawKun = k.kunyomi?.[0]?.replace(/\./g, '') || ''
            finalReading = rawKun || k.onyomi?.[0] || ''
            if (!finalHanViet && k.hanViet) {
              finalHanViet = k.hanViet.split(',')[0].trim().toUpperCase()
            }
            if (!suggestedMeaning && k.meaningVi) {
              suggestedMeaning = k.meaningVi
            }
          }
        }

        // 4. Trích xuất âm Hán Việt từ danh sách Kanjis trả về
        if (!finalHanViet && /[\u4e00-\u9faf]/.test(trimmed)) {
          const kanjiChars = trimmed.match(/[\u4e00-\u9faf]/g) || []
          if (kanjiChars.length > 0) {
            const parts = await Promise.all(
              kanjiChars.map(async (ch) => {
                const found = kanjis.find((k) => k.character === ch)
                if (found?.hanViet) {
                  return found.hanViet.split(',')[0].trim().toUpperCase()
                }
                try {
                  const res = await dictionaryService.getKanjiDetail(ch)
                  const hv = res.data?.hanViet || ''
                  return hv.split(',')[0].trim().toUpperCase()
                } catch {
                  return ''
                }
              })
            )
            finalHanViet = parts.filter(Boolean).join(' ')
          }
        }
      }

      // Tránh race condition: nếu người dùng đã gõ sang từ khác, bỏ qua kết quả tra cứu cũ
      if (form.getFieldValue('word')?.trim() !== trimmed) {
        return
      }

      const updates: Record<string, string> = {}

      if (targetField === 'reading') {
        updates.reading = finalReading || (isPureKana ? trimmed : '')
      } else if (targetField === 'sinoVietnamese') {
        updates.sinoVietnamese = finalHanViet || ''
      } else {
        if (!userModifiedRef.current.reading || isManual) {
          updates.reading = finalReading || (isPureKana ? trimmed : '')
        }
        if (!userModifiedRef.current.sinoVietnamese || isManual) {
          updates.sinoVietnamese = finalHanViet || ''
        }
        if (!userModifiedRef.current.meaning || isManual) {
          if (suggestedMeaning) {
            updates.meaning = suggestedMeaning
          } else if (isManual) {
            updates.meaning = ''
          }
        }
        if (!userModifiedRef.current.partOfSpeech || isManual) {
          updates.partOfSpeech = suggestedPartOfSpeech
        }
      }

      form.setFieldsValue(updates)
    } catch (err) {
      console.warn('Auto fill error:', err)
    } finally {
      setIsAutoFilling(false)
    }
  }

  const handleWordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    // Nếu đang sửa từ và quay lại đúng từ ban đầu, khôi phục dữ liệu gốc và khóa cờ chỉnh sửa
    if (editingWord && val.trim() === editingWord.word?.trim()) {
      form.setFieldsValue({
        reading: editingWord.reading,
        sinoVietnamese: editingWord.sinoVietnamese || editingWord.hanViet,
        meaning: editingWord.meaning,
        partOfSpeech: ALLOWED_POS.includes(editingWord.partOfSpeech as any)
          ? editingWord.partOfSpeech
          : 'Từ vựng',
      })
      userModifiedRef.current = {
        reading: true,
        sinoVietnamese: true,
        meaning: true,
        partOfSpeech: true,
      }
      return
    }

    // Khi người dùng thay đổi từ vựng sang từ khác, reset cờ chỉnh sửa thủ công để các trường tự động cập nhật theo từ mới
    if (editingWord && val.trim() !== editingWord.word?.trim()) {
      userModifiedRef.current = {
        reading: false,
        sinoVietnamese: false,
        meaning: false,
        partOfSpeech: false,
      }
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }
    debounceTimerRef.current = setTimeout(() => {
      triggerAutoFill(val, false)
    }, 300)
  }

  const handleWordBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    // Nếu đang sửa từ có sẵn và từ không đổi, không tự động điền lại khi blur để tránh ghi đè dữ liệu người dùng
    if (editingWord && e.target.value.trim() === editingWord.word?.trim()) {
      return
    }
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }
    triggerAutoFill(e.target.value, false)
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      onSubmit(values)
    } catch {
      // validation error
    }
  }

  return (
    <Modal
      title={
        <div className="text-base font-bold text-gray-900 pb-1">
          {editingWord ? 'Chỉnh sửa từ vựng' : 'Thêm từ vựng mới'}
        </div>
      }
      open={open}
      onCancel={onCancel}
      footer={null}
      centered
      width={480}
      destroyOnClose
      className="mazii-modal"
    >
      <Form form={form} layout="vertical" className="space-y-3 pt-2">
        {/* 1. Từ vựng * (Trường nhập liệu chính duy nhất) */}
        <Form.Item
          name="word"
          label={
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-gray-700">
                Từ vựng <span className="text-red-500">*</span>
              </span>
              <Button
                type="link"
                size="small"
                icon={<ThunderboltOutlined />}
                loading={isAutoFilling}
                onClick={() => triggerAutoFill(form.getFieldValue('word') || '', true)}
                className="!p-0 !h-auto text-[11px] text-blue-600 hover:text-blue-700 flex items-center gap-1 font-medium"
              >
                Gợi ý tra từ
              </Button>
            </div>
          }
          rules={[{ required: true, message: 'Vui lòng nhập từ vựng' }]}
          // Dấu * đã nằm trong nhãn (cạnh nút Gợi ý) — tắt dấu tự động để không bị lặp; rules vẫn kiểm tra
          required={false}
          className="!mb-3"
        >
          <Input
            placeholder="Nhập từ vựng (ví dụ: 日本語, 食べる, 避雷針...)"
            autoFocus={!editingWord}
            onChange={handleWordChange}
            onBlur={handleWordBlur}
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
          />
        </Form.Item>

        {/* 2. Phiên âm & Âm Hán Việt (Tự động điền theo từ vựng, người dùng VẪN ĐƯỢC CHỈNH SỬA, không để readOnly) */}
        <div className="grid grid-cols-2 gap-3 !mb-3">
          <Form.Item
            name="reading"
            label={
              <div className="flex items-center justify-between w-full">
                <span className="text-xs font-semibold text-gray-700">Phiên âm</span>
                <button
                  type="button"
                  title="Bấm để tự động điền lại phiên âm"
                  onClick={() => {
                    userModifiedRef.current.reading = false
                    triggerAutoFill(form.getFieldValue('word') || '', false, 'reading')
                  }}
                  className="text-[10px] text-blue-600 bg-blue-50 hover:bg-blue-100 px-1.5 py-0.5 rounded font-medium flex items-center gap-1 transition-colors cursor-pointer border-0"
                >
                  {isAutoFilling ? (
                    <Spin indicator={<LoadingOutlined style={{ fontSize: 10 }} spin />} />
                  ) : (
                    <ThunderboltOutlined />
                  )}
                  Tự động điền
                </button>
              </div>
            }
            className="!mb-0"
          >
            <Input
              placeholder="Ví dụ: にほんご, たべる"
              onChange={(e) => {
                userModifiedRef.current.reading = e.target.value.trim().length > 0
              }}
              className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
            />
          </Form.Item>

          <Form.Item
            name="sinoVietnamese"
            label={
              <div className="flex items-center justify-between w-full">
                <span className="text-xs font-semibold text-gray-700">Âm Hán Việt</span>
                <button
                  type="button"
                  title="Bấm để tự động điền lại âm Hán Việt"
                  onClick={() => {
                    userModifiedRef.current.sinoVietnamese = false
                    triggerAutoFill(form.getFieldValue('word') || '', false, 'sinoVietnamese')
                  }}
                  className="text-[10px] text-emerald-600 bg-emerald-50 hover:bg-emerald-100 px-1.5 py-0.5 rounded font-medium flex items-center gap-1 transition-colors cursor-pointer border-0"
                >
                  {isAutoFilling ? (
                    <Spin indicator={<LoadingOutlined style={{ fontSize: 10 }} spin />} />
                  ) : (
                    <ThunderboltOutlined />
                  )}
                  Tự động điền
                </button>
              </div>
            }
            className="!mb-0"
          >
            <Input
              placeholder="Ví dụ: NHẬT BẢN NGỮ, THỰC"
              onChange={(e) => {
                userModifiedRef.current.sinoVietnamese = e.target.value.trim().length > 0
              }}
              className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500 font-medium"
            />
          </Form.Item>
        </div>

        {/* 3. Nghĩa của từ * */}
        <Form.Item
          name="meaning"
          label={
            <span className="text-xs font-semibold text-gray-700">
              Nghĩa của từ
            </span>
          }
          rules={[{ required: true, message: 'Vui lòng nhập nghĩa của từ' }]}
          className="!mb-3"
        >
          <Input.TextArea
            rows={3}
            placeholder="Nhập nghĩa của từ"
            onChange={(e) => {
              userModifiedRef.current.meaning = e.target.value.trim().length > 0
            }}
            className="rounded-xl border-gray-200 text-sm focus:border-blue-500"
          />
        </Form.Item>

        {/* 4. Từ loại * (CHỈ GỒM ĐÚNG 3 LỰA CHỌN: Từ vựng, Chữ Hán, Ngữ pháp) */}
        <Form.Item
          name="partOfSpeech"
          label={
            <span className="text-xs font-semibold text-gray-700">
              Từ loại
            </span>
          }
          rules={[{ required: true, message: 'Vui lòng chọn từ loại' }]}
          className="!mb-3"
        >
          <Select
            className="w-full h-11"
            onChange={() => {
              userModifiedRef.current.partOfSpeech = true
            }}
            options={[
              { label: 'Từ vựng', value: 'Từ vựng' },
              { label: 'Chữ Hán', value: 'Chữ Hán' },
              { label: 'Ngữ pháp', value: 'Ngữ pháp' },
            ]}
          />
        </Form.Item>

        {/* 5. Ghi chú cá nhân (tùy chọn) */}
        <Form.Item
          name="userNote"
          label={<span className="text-xs font-semibold text-gray-700">Ghi chú</span>}
          className="!mb-3"
        >
          <Input
            placeholder="Nhập ghi chú cá nhân"
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
          />
        </Form.Item>

        {/* 6. Footer Buttons chuẩn Mazii */}
        <div className="grid grid-cols-2 gap-3 pt-3">
          <Button
            size="large"
            onClick={onCancel}
            className="h-11 rounded-xl bg-gray-100 hover:bg-gray-200 border-none text-gray-700 font-medium"
          >
            Hủy
          </Button>
          <Button
            type="primary"
            size="large"
            loading={submitting}
            onClick={handleOk}
            className="h-11 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium"
          >
            {editingWord ? 'Lưu thay đổi' : 'Thêm'}
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

export default WordFormModal