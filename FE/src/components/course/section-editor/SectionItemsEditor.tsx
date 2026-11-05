import React from 'react'
import type { LessonSection } from '@/types'
import { KanjiItemsEditor } from './KanjiItemsEditor'
import { VocabItemsEditor } from './VocabItemsEditor'
import { GrammarItemsEditor } from './GrammarItemsEditor'

interface SectionItemsEditorProps {
  lessonId: number
  section: LessonSection
}

/** Editor items (Kanji / Từ vựng / Ngữ pháp) tương ứng với loại của section. */
export const SectionItemsEditor: React.FC<SectionItemsEditorProps> = ({ lessonId, section }) => {
  switch (section.sectionType) {
    case 'KANJI':
      return <KanjiItemsEditor lessonId={lessonId} sectionId={section.id} items={section.kanjiItems ?? []} />
    case 'VOCABULARY':
      return <VocabItemsEditor lessonId={lessonId} sectionId={section.id} items={section.vocabularyItems ?? []} />
    case 'GRAMMAR':
      return <GrammarItemsEditor lessonId={lessonId} sectionId={section.id} items={section.grammarItems ?? []} />
    default:
      return <p className="text-sm text-rose-600">Loại mục không được hỗ trợ: {String(section.sectionType)}</p>
  }
}
