import React, { useMemo, useState } from 'react'
import { Button, Modal, Steps, Switch, Table, Tabs, Tag, Upload } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { AlertTriangle, CheckCircle2, Download, FileSpreadsheet, Info, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import type { SectionImportApi } from '@/services/sectionImport.service'
import type { ImportRowResult, ImportRowStatus, SectionImportCommitResponse } from '@/types'
import { useSectionImport } from '@/hooks'

const MAX_FILE_BYTES = 2 * 1024 * 1024

export interface ImportStepsLayoutProps {
  open: boolean
  onClose: () => void
  lessonId: number
  sectionId: number
  /** Service import riêng của loại section */
  api: SectionImportApi
  /** Tiêu đề modal, vd "Import từ vựng từ Excel" */
  title: string
  /** Quy tắc riêng của mẫu, hiện ở bước 1 */
  rules: React.ReactNode[]
  /** Tên + cột bảng xem trước cho sheet chính / sheet phụ */
  itemSheet: { name: string; label: string; columns: ColumnsType<ImportRowResult> }
  childSheet: { name: string; label: string; columns: ColumnsType<ImportRowResult> }
  /** Đơn vị đếm, vd "từ vựng" / "câu ví dụ" */
  itemUnit: string
  childUnit: string
}

const STATUS_TAG: Record<ImportRowStatus, { color: string; label: string }> = {
  OK: { color: 'green', label: 'Hợp lệ' },
  WARNING: { color: 'gold', label: 'Lưu ý' },
  ERROR: { color: 'red', label: 'Lỗi' },
}

/** Cột chung: số dòng + trạng thái ở đầu, thông báo ở cuối. */
const withCommonColumns = (columns: ColumnsType<ImportRowResult>): ColumnsType<ImportRowResult> => [
  { title: 'Dòng', dataIndex: 'rowNumber', width: 64, fixed: 'left' },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    width: 96,
    render: (s: ImportRowStatus) => <Tag color={STATUS_TAG[s].color}>{STATUS_TAG[s].label}</Tag>,
  },
  ...columns,
  {
    title: 'Thông báo',
    dataIndex: 'messages',
    width: 280,
    render: (messages: string[], row) =>
      messages.length === 0 ? (
        <span className="text-gray-300">—</span>
      ) : (
        <ul className={`m-0 list-disc pl-4 text-xs ${row.status === 'ERROR' ? 'text-rose-600' : 'text-amber-600'}`}>
          {messages.map((m, i) => (
            <li key={i}>{m}</li>
          ))}
        </ul>
      ),
  },
]

/**
 * Khung 3 bước dùng chung cho modal import: Tải mẫu & chọn file → Xem trước → Kết quả.
 * Mỗi loại section (Kanji / Từ vựng / Ngữ pháp) tự truyền service, quy tắc và cột bảng riêng.
 */
export const ImportStepsLayout: React.FC<ImportStepsLayoutProps> = ({
  open,
  onClose,
  lessonId,
  sectionId,
  api,
  title,
  rules,
  itemSheet,
  childSheet,
  itemUnit,
  childUnit,
}) => {
  const { downloadTemplate, preview, commit } = useSectionImport(api, lessonId, sectionId)
  const [file, setFile] = useState<File | null>(null)
  const [onlyProblems, setOnlyProblems] = useState(false)
  const [result, setResult] = useState<SectionImportCommitResponse | null>(null)

  const previewData = preview.data
  const step = result ? 2 : previewData ? 1 : 0

  const reset = () => {
    setFile(null)
    setResult(null)
    setOnlyProblems(false)
    preview.reset()
    commit.reset()
  }

  const handleClose = () => {
    reset()
    onClose()
  }

  const handleSelectFile = (selected: File) => {
    if (!selected.name.toLowerCase().endsWith('.xlsx')) {
      toast.error('Chỉ nhận file Excel .xlsx')
      return Upload.LIST_IGNORE
    }
    if (selected.size > MAX_FILE_BYTES) {
      toast.error('File vượt quá 2MB')
      return Upload.LIST_IGNORE
    }
    setFile(selected)
    preview.mutate(selected)
    return false // không để antd tự upload
  }

  const handleCommit = () => {
    if (!file) return
    commit.mutate(file, { onSuccess: (data) => setResult(data ?? null) })
  }

  const filterRows = (rows: ImportRowResult[]) => (onlyProblems ? rows.filter((r) => r.status !== 'OK') : rows)

  const tabs = useMemo(() => {
    if (!previewData) return []
    const countErrors = (rows: ImportRowResult[]) => rows.filter((r) => r.status === 'ERROR').length
    const label = (text: string, rows: ImportRowResult[]) => {
      const errors = countErrors(rows)
      return (
        <span>
          {text} ({rows.length}){errors > 0 && <Tag color="red" className="ml-2">{errors} lỗi</Tag>}
        </span>
      )
    }
    return [
      {
        key: 'items',
        label: label(itemSheet.label, previewData.itemRows),
        children: (
          <Table<ImportRowResult>
            size="small"
            rowKey={(r) => `${r.sheet}-${r.rowNumber}`}
            columns={withCommonColumns(itemSheet.columns)}
            dataSource={filterRows(previewData.itemRows)}
            pagination={{ pageSize: 10, hideOnSinglePage: true }}
            scroll={{ x: 'max-content' }}
          />
        ),
      },
      {
        key: 'children',
        label: label(childSheet.label, previewData.childRows),
        children:
          previewData.childRows.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500">
              Sheet <b>{childSheet.name}</b> không có dữ liệu.
            </p>
          ) : (
            <Table<ImportRowResult>
              size="small"
              rowKey={(r) => `${r.sheet}-${r.rowNumber}`}
              columns={withCommonColumns(childSheet.columns)}
              dataSource={filterRows(previewData.childRows)}
              pagination={{ pageSize: 10, hideOnSinglePage: true }}
              scroll={{ x: 'max-content' }}
            />
          ),
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [previewData, onlyProblems, itemSheet, childSheet])

  return (
    <Modal
      open={open}
      onCancel={handleClose}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <FileSpreadsheet className="h-5 w-5 text-emerald-600" />
          <span>{title}</span>
        </div>
      }
      width={1100}
      footer={null}
      destroyOnHidden
      mask={{ closable: !commit.isPending }}
    >
      <Steps
        size="small"
        current={step}
        className="mb-5 mt-2"
        items={[{ title: 'Tải mẫu & chọn file' }, { title: 'Kiểm tra dữ liệu' }, { title: 'Hoàn tất' }]}
      />

      {/* ---------- Bước 1: tải mẫu + chọn file ---------- */}
      {step === 0 && (
        <div className="space-y-4">
          <div className="rounded-lg border border-indigo-100 bg-indigo-50/60 p-4 text-sm text-gray-700">
            <div className="mb-2 flex items-center gap-2 font-semibold text-indigo-700">
              <Info className="h-4 w-4" /> Chỉ nhận file theo đúng mẫu của hệ thống
            </div>
            <ul className="m-0 list-disc space-y-1 pl-5">
              {rules.map((rule, i) => (
                <li key={i}>{rule}</li>
              ))}
            </ul>
            <Button
              className="mt-3"
              icon={<Download className="h-4 w-4" />}
              loading={downloadTemplate.isPending}
              onClick={() => downloadTemplate.mutate()}
            >
              Tải file Excel mẫu
            </Button>
          </div>

          <Upload.Dragger
            accept=".xlsx"
            multiple={false}
            showUploadList={false}
            beforeUpload={handleSelectFile}
            disabled={preview.isPending}
          >
            <div className="py-4">
              <FileSpreadsheet className="mx-auto mb-2 h-10 w-10 text-emerald-500" />
              <p className="font-medium text-gray-800">
                {preview.isPending ? `Đang kiểm tra ${file?.name ?? 'file'}…` : 'Kéo thả hoặc bấm để chọn file .xlsx'}
              </p>
              <p className="text-xs text-gray-500">Tối đa 2MB. Hệ thống chỉ kiểm tra, chưa ghi dữ liệu.</p>
            </div>
          </Upload.Dragger>
        </div>
      )}

      {/* ---------- Bước 2: xem trước ---------- */}
      {step === 1 && previewData && (
        <div className="space-y-3">
          {previewData.canCommit ? (
            <div className="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
              <span>
                File hợp lệ: <b>{previewData.totalItems}</b> {itemUnit}, <b>{previewData.totalChildren}</b> {childUnit}.
                {previewData.warningRows > 0 && ` Có ${previewData.warningRows} dòng cần lưu ý, vẫn có thể import.`}
              </span>
            </div>
          ) : (
            <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
              <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>
                Có <b>{previewData.errorRows}</b> dòng lỗi. Hãy sửa trong file Excel rồi chọn lại file — hệ thống không
                ghi dữ liệu khi file còn lỗi.
              </span>
            </div>
          )}

          <div className="flex items-center justify-between text-sm text-gray-600">
            <span className="truncate">
              File: <b>{file?.name}</b>
            </span>
            <label className="flex cursor-pointer items-center gap-2">
              <Switch size="small" checked={onlyProblems} onChange={setOnlyProblems} />
              Chỉ hiện dòng lỗi / lưu ý
            </label>
          </div>

          <Tabs items={tabs} />

          <div className="flex justify-end gap-2 border-t pt-3">
            <Button onClick={reset} disabled={commit.isPending}>
              Chọn file khác
            </Button>
            <Button
              type="primary"
              disabled={!previewData.canCommit}
              loading={commit.isPending}
              onClick={handleCommit}
            >
              Import {previewData.totalItems} {itemUnit}
            </Button>
          </div>
        </div>
      )}

      {/* ---------- Bước 3: kết quả ---------- */}
      {step === 2 && result && (
        <div className="py-8 text-center">
          <CheckCircle2 className="mx-auto mb-3 h-14 w-14 text-emerald-500" />
          <p className="text-lg font-semibold text-gray-900">Import thành công</p>
          <p className="text-sm text-gray-600">
            Đã thêm <b>{result.importedItems}</b> {itemUnit}
            {result.importedChildren > 0 && (
              <>
                {' '}
                và <b>{result.importedChildren}</b> {childUnit}
              </>
            )}{' '}
            vào cuối section.
          </p>
          <div className="mt-5 flex justify-center gap-2">
            <Button onClick={reset}>Import file khác</Button>
            <Button type="primary" onClick={handleClose}>
              Đóng
            </Button>
          </div>
        </div>
      )}

      {commit.isError && step === 1 && (
        <p className="mt-2 flex items-center gap-1 text-xs text-rose-600">
          <AlertTriangle className="h-3.5 w-3.5" /> Import thất bại, dữ liệu chưa được ghi. Thử kiểm tra lại file.
        </p>
      )}
    </Modal>
  )
}
