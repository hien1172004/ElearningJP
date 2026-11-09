import type { NotebookItemType } from '@/types'

/** Thông tin hiển thị theo loại thẻ sổ tay (Từ vựng / Chữ Hán / Ngữ pháp). */
export const NOTEBOOK_ITEM_TYPES: NotebookItemType[] = ['VOCAB', 'KANJI', 'GRAMMAR']

export const NOTEBOOK_ITEM_TYPE_META: Record<
  NotebookItemType,
  { label: string; short: string; tagColor: string; chipClass: string }
> = {
  VOCAB: {
    label: 'Từ vựng',
    short: 'từ',
    tagColor: 'green',
    chipClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  },
  KANJI: {
    label: 'Chữ Hán',
    short: 'chữ Hán',
    tagColor: 'orange',
    chipClass: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  GRAMMAR: {
    label: 'Ngữ pháp',
    short: 'ngữ pháp',
    tagColor: 'magenta',
    chipClass: 'bg-rose-50 text-rose-700 border-rose-200',
  },
}

/** Thẻ cũ (trước V54) chưa có itemType → coi là Từ vựng. */
export const itemTypeOf = (item: { itemType?: NotebookItemType | null }): NotebookItemType =>
  item.itemType ?? 'VOCAB'

/** "32 từ · 10 chữ Hán · 5 ngữ pháp" — bỏ loại có 0 thẻ; sổ cũ chưa có bộ đếm theo loại → "N từ". */
export const formatNotebookCounts = (nb: {
  wordCount: number
  vocabCount?: number
  kanjiCount?: number
  grammarCount?: number
}): string => {
  if (nb.vocabCount == null && nb.kanjiCount == null && nb.grammarCount == null) {
    return `${nb.wordCount ?? 0} từ`
  }
  const parts = [
    nb.vocabCount ? `${nb.vocabCount} từ` : null,
    nb.kanjiCount ? `${nb.kanjiCount} chữ Hán` : null,
    nb.grammarCount ? `${nb.grammarCount} ngữ pháp` : null,
  ].filter(Boolean)
  return parts.length > 0 ? parts.join(' · ') : '0 thẻ'
}
