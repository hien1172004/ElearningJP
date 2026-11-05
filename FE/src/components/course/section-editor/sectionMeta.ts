import type { LessonSection, LessonSectionType } from '@/types'

export const SECTION_TYPE_LABEL: Record<LessonSectionType, string> = {
  KANJI: 'Chữ Hán',
  VOCABULARY: 'Từ vựng',
  GRAMMAR: 'Ngữ pháp',
}

/** Số mục nội dung trong một section (theo đúng loại của section). */
export const countSectionItems = (section: LessonSection): number => {
  switch (section.sectionType) {
    case 'KANJI':
      return section.kanjiItems?.length ?? 0
    case 'VOCABULARY':
      return section.vocabularyItems?.length ?? 0
    case 'GRAMMAR':
      return section.grammarItems?.length ?? 0
    default:
      return 0
  }
}
