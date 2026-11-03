import React, { useState, useRef } from 'react'
import { Modal, Button, message, Spin } from 'antd'
import {
  InfoCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { ExcelImportResultResponse } from '@/types'

interface WordImportModalProps {
  open: boolean
  submitting?: boolean
  onCancel: () => void
  onImport: (file: File) => Promise<ExcelImportResultResponse | void>
}

const MAX_FILE_SIZE_MB = 5
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

export const WordImportModal: React.FC<WordImportModalProps> = ({
  open,
  submitting = false,
  onCancel,
  onImport,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [importResult, setImportResult] = useState<ExcelImportResultResponse | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleReset = () => {
    setSelectedFile(null)
    setImportResult(null)
    setIsDragging(false)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleClose = () => {
    handleReset()
    onCancel()
  }

  const validateAndImportFile = async (file: File) => {
    const isExcel =
      file.name.endsWith('.xlsx') ||
      file.name.endsWith('.xls') ||
      file.type ===
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
      file.type === 'application/vnd.ms-excel'

    if (!isExcel) {
      message.error('Vui lòng chỉ tải lên file Excel (.xlsx hoặc .xls)!')
      return
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      message.error(`Dung lượng file vượt quá giới hạn ${MAX_FILE_SIZE_MB}MB!`)
      return
    }

    setSelectedFile(file)
    try {
      const result = await onImport(file)
      if (result) {
        setImportResult(result)
        const success = result.successCount ?? result.importedCount ?? 0
        message.success(`Đã nhập thành công ${success} từ vựng!`)
      }
    } catch {
      // Error handled in parent/service
    }
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      validateAndImportFile(file)
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
    const file = e.dataTransfer.files?.[0]
    if (file) {
      validateAndImportFile(file)
    }
  }

  return (
    <Modal
      title={
        <div className="text-base font-bold text-gray-900 pb-1">
          {importResult ? 'Kết quả nhập dữ liệu' : 'Chọn file có cấu trúc dạng sau'}
        </div>
      }
      open={open}
      onCancel={handleClose}
      destroyOnClose
      width={720}
      centered
      className="mazii-modal"
      footer={
        importResult ? (
          <div className="flex justify-end pt-2">
            <Button
              type="primary"
              size="large"
              onClick={handleClose}
              className="h-10 px-6 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium"
            >
              Hoàn tất
            </Button>
          </div>
        ) : (
          <div className="flex justify-end gap-3 pt-2">
            <Button
              size="large"
              onClick={handleClose}
              className="h-10 px-6 rounded-xl bg-gray-100 hover:bg-gray-200 border-none text-gray-700 font-medium"
            >
              Hủy
            </Button>
            <Button
              type="primary"
              size="large"
              loading={submitting}
              onClick={() => fileInputRef.current?.click()}
              className="h-10 px-6 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium"
            >
              Tiếp tục
            </Button>
          </div>
        )
      }
    >
      {/* Input file ẩn, kích hoạt khi bấm nút Tiếp tục */}
      <input
        type="file"
        ref={fileInputRef}
        accept=".xlsx, .xls"
        onChange={handleFileInputChange}
        className="hidden"
      />

      <div
        className="space-y-4 my-2"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {!importResult ? (
          <>
            {/* Bảng cấu trúc mẫu chuẩn Mazii */}
            <div
              className={`border rounded-xl overflow-hidden shadow-2xs transition-colors ${
                isDragging
                  ? 'border-blue-500 bg-blue-50/30'
                  : 'border-gray-200 bg-white'
              }`}
            >
              <table className="w-full text-xs text-left border-collapse">
                <thead className="bg-gray-50 border-b border-gray-200 text-gray-700 font-semibold">
                  <tr>
                    <th className="py-2.5 px-3 border-r border-gray-200">
                      Cột 1: Từ vựng (Bắt buộc)
                    </th>
                    <th className="py-2.5 px-3 border-r border-gray-200">
                      Cột 2: Phonetic
                    </th>
                    <th className="py-2.5 px-3 border-r border-gray-200">
                      Cột 3: Nghĩa (Bắt buộc)
                    </th>
                    <th className="py-2.5 px-3">Cột 4: Ghi chú</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-gray-800">
                  <tr>
                    <td className="py-2.5 px-3 border-r border-gray-100 font-medium">
                      歴然
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100 text-gray-600">
                      れきぜん
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100">
                      Mùi hương nhẹ nhàng, thoang thoảng
                    </td>
                    <td className="py-2.5 px-3 text-gray-400">-</td>
                  </tr>
                  <tr className="bg-gray-50/40">
                    <td className="py-2.5 px-3 border-r border-gray-100 font-medium">
                      勉強
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100 text-gray-600">
                      べんきょう
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100">
                      Học tập, việc học
                    </td>
                    <td className="py-2.5 px-3 text-gray-600">
                      Thường đi với động từ する (勉強する)
                    </td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3 border-r border-gray-100 font-medium">
                      V / A い / A な / N の N修飾形 + せいか
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100 text-gray-400">
                      -
                    </td>
                    <td className="py-2.5 px-3 border-r border-gray-100 leading-relaxed">
                      <div>V / A い / A な / N の N修飾形 + せいか</div>
                      <div className="text-gray-500 text-[11px]">
                        N dạng biến đổi của V/Ai/Ana/N + Seika
                      </div>
                    </td>
                    <td className="py-2.5 px-3 text-gray-400">-</td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* Dòng lưu ý theo đúng yêu cầu */}
            <div className="flex items-start gap-2 text-xs text-gray-600 pt-1">
              <InfoCircleOutlined className="text-gray-400 mt-0.5 text-sm shrink-0" />
              <span className="leading-relaxed">
                Lưu ý: Hệ thống chỉ xử lý tối đa 500 dòng dữ liệu trong một lần import file Excel, vui lòng kiểm tra và chia nhỏ file nếu dữ liệu vượt quá giới hạn.
              </span>
            </div>

            {/* Trạng thái đang tải lên hoặc file đã chọn */}
            {submitting && (
              <div className="flex items-center justify-center gap-2 p-3 bg-blue-50/60 rounded-xl text-blue-700 text-xs font-medium">
                <Spin size="small" />
                <span>
                  Đang xử lý nhập dữ liệu từ file {selectedFile?.name}...
                </span>
              </div>
            )}
          </>
        ) : (
          /* Báo cáo kết quả sau khi nhập thành công */
          <div className="space-y-4 p-4 bg-gray-50 rounded-xl border border-gray-100">
            <div className="flex items-center gap-2 text-emerald-600 font-semibold text-base">
              <CheckCircleOutlined className="text-xl" />
              <span>Đã xử lý nhập dữ liệu hoàn tất</span>
            </div>

            <div className="grid grid-cols-3 gap-3 text-center text-xs">
              <div className="bg-white p-3 rounded-xl border border-gray-100 shadow-2xs">
                <div className="text-gray-400 mb-1">Tổng số dòng</div>
                <div className="text-lg font-bold text-gray-800">
                  {importResult.totalProcessed ?? importResult.totalRows ?? 0}
                </div>
              </div>
              <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-2xs">
                <div className="text-emerald-500 mb-1">Đã thêm thành công</div>
                <div className="text-lg font-bold text-emerald-600">
                  {importResult.successCount ?? importResult.importedCount ?? 0}
                </div>
              </div>
              <div className="bg-white p-3 rounded-xl border border-amber-100 shadow-2xs">
                <div className="text-amber-500 mb-1">Bỏ qua / Lỗi</div>
                <div className="text-lg font-bold text-amber-600">
                  {importResult.errorCount ?? importResult.skippedCount ?? 0}
                </div>
              </div>
            </div>

            {importResult.errorMessages && importResult.errorMessages.length > 0 && (
              <div className="mt-2">
                <div className="text-xs font-medium text-amber-700 flex items-center gap-1 mb-1.5">
                  <WarningOutlined /> Chi tiết các dòng bị bỏ qua hoặc lỗi:
                </div>
                <div className="max-h-36 overflow-y-auto bg-amber-50/80 p-2.5 rounded-xl text-[11px] text-amber-800 space-y-1 border border-amber-100">
                  {importResult.errorMessages.map((msg, idx) => (
                    <div key={idx}>• {msg}</div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  )
}

export default WordImportModal