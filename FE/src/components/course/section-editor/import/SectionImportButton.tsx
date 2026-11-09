import React, { useState } from 'react'
import { useCourseEditLocked } from '@/components/course/edit-lock'
import { Button, Tooltip } from 'antd'
import { Download, FileSpreadsheet } from 'lucide-react'
import type { LessonSection } from '@/types'
import { grammarImportService, kanjiImportService, vocabularyImportService } from '@/services'
import type { SectionImportApi } from '@/services/sectionImport.service'
import { useSectionExport } from '@/hooks'
import { toSlug } from '@/utils'
import { KanjiImportModal } from './KanjiImportModal'
import { VocabularyImportModal } from './VocabularyImportModal'
import { GrammarImportModal } from './GrammarImportModal'

interface SectionImportButtonProps {
  lessonId: number
  section: LessonSection
}

/** Service + tiền tố tên file xuất theo loại section. */
const EXPORT_CONFIG: Record<LessonSection['sectionType'], { api: SectionImportApi; prefix: string }> = {
  KANJI: { api: kanjiImportService, prefix: 'chu_han' },
  VOCABULARY: { api: vocabularyImportService, prefix: 'tu_vung' },
  GRAMMAR: { api: grammarImportService, prefix: 'ngu_phap' },
}

/** Số item hiện có của section (undefined nếu dữ liệu item chưa được tải kèm). */
const itemCount = (section: LessonSection): number | undefined =>
  ({
    KANJI: section.kanjiItems,
    VOCABULARY: section.vocabularyItems,
    GRAMMAR: section.grammarItems,
  })[section.sectionType]?.length

/** Nút "Import Excel" (modal import đúng mẫu của loại section) + "Xuất Excel" (cùng định dạng mẫu). */
export const SectionImportButton: React.FC<SectionImportButtonProps> = ({ lessonId, section }) => {
  // Khóa đang chờ duyệt: tạm khóa các thao tác sửa nội dung
  const locked = useCourseEditLocked()
  const [open, setOpen] = useState(false)
  const modalProps = { open, onClose: () => setOpen(false), lessonId, sectionId: section.id }
  const exportConfig = EXPORT_CONFIG[section.sectionType]
  const exportMutation = useSectionExport(exportConfig.api, section.id)
  const empty = itemCount(section) === 0

  const handleExport = () => {
    const slug = toSlug(section.title ?? '') || `muc-${section.id}`
    exportMutation.mutate(`${exportConfig.prefix}_${slug}.xlsx`)
  }

  return (
    <>
      <Button
        size="small"
        icon={<FileSpreadsheet className="h-4 w-4 text-emerald-600" />}
        disabled={locked}
        onClick={() => setOpen(true)}
      >
        Import Excel
      </Button>
      <Tooltip title={empty ? 'Mục chưa có dữ liệu để xuất' : 'Xuất dữ liệu mục này ra Excel (cùng định dạng mẫu import)'}>
        <Button
          size="small"
          icon={<Download className="h-4 w-4 text-sky-600" />}
          onClick={handleExport}
          loading={exportMutation.isPending}
          disabled={empty}
        >
          Xuất Excel
        </Button>
      </Tooltip>
      {section.sectionType === 'KANJI' && <KanjiImportModal {...modalProps} />}
      {section.sectionType === 'VOCABULARY' && <VocabularyImportModal {...modalProps} />}
      {section.sectionType === 'GRAMMAR' && <GrammarImportModal {...modalProps} />}
    </>
  )
}
