import React, { useState } from 'react'
import { Button } from 'antd'
import { FileSpreadsheet } from 'lucide-react'
import type { LessonSection } from '@/types'
import { KanjiImportModal } from './KanjiImportModal'
import { VocabularyImportModal } from './VocabularyImportModal'
import { GrammarImportModal } from './GrammarImportModal'

interface SectionImportButtonProps {
  lessonId: number
  section: LessonSection
}

/** Nút "Import Excel" + modal import đúng mẫu của loại section (Kanji / Từ vựng / Ngữ pháp). */
export const SectionImportButton: React.FC<SectionImportButtonProps> = ({ lessonId, section }) => {
  const [open, setOpen] = useState(false)
  const modalProps = { open, onClose: () => setOpen(false), lessonId, sectionId: section.id }

  return (
    <>
      <Button
        size="small"
        icon={<FileSpreadsheet className="h-4 w-4 text-emerald-600" />}
        onClick={() => setOpen(true)}
      >
        Import Excel
      </Button>
      {section.sectionType === 'KANJI' && <KanjiImportModal {...modalProps} />}
      {section.sectionType === 'VOCABULARY' && <VocabularyImportModal {...modalProps} />}
      {section.sectionType === 'GRAMMAR' && <GrammarImportModal {...modalProps} />}
    </>
  )
}
