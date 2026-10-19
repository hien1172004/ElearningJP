import React from 'react'
import { BookOpen } from 'lucide-react'
import type { LessonSection } from '@/types'
import { KanjiSectionView } from './KanjiSectionView'
import { VocabSectionView } from './VocabSectionView'
import { GrammarSectionView } from './GrammarSectionView'

interface SectionRendererProps {
  section: LessonSection
}

export const SectionRenderer: React.FC<SectionRendererProps> = ({ section }) => {
  switch (section.sectionType) {
    case 'KANJI':
      return <KanjiSectionView section={section} />
    case 'VOCABULARY':
      return <VocabSectionView section={section} />
    case 'GRAMMAR':
      return <GrammarSectionView section={section} />
    default:
      return (
        <div className="text-center py-6 text-sm text-gray-500">
          <BookOpen className="w-8 h-8 mx-auto mb-2 text-gray-300" />
          <p>Loại section không xác định: {section.sectionType}</p>
        </div>
      )
  }
}
