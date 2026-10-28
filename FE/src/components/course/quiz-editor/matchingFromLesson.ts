import type { LessonDetail } from '@/types'
import type { MatchingSide } from './MatchingPairsEditor'

/**
 * ============================================================================
 * MATCHING — lấy dữ liệu từ lesson
 * ============================================================================
 *
 * Helper trích TẤT CẢ items (Kanji + Từ vựng + Ngữ pháp) từ 1 lesson để
 * dùng làm danh sách "candidate" cho câu hỏi MATCHING.
 *
 * Luồng sử dụng:
 *   1. Mở form tạo/sửa câu hỏi MATCHING.
 *   2. Editor tự động gọi `buildCandidatesFromLesson(lesson)` → list candidates.
 *   3. Teacher bấm "Tự động lấy tất cả" → gọi `autoPairCandidates(candidates)`
 *      → sinh ra {left, right} matchingData.
 *   4. Teacher có thể tinh chỉnh từng dòng thông qua dropdown AutoComplete.
 *
 * Lợi ích: không phải gõ tay từng cặp → đồng bộ 100% với nội dung lesson,
 * giảm sai chính tả, tiết kiệm thời gian.
 */

// ============================================================================
// Types
// ============================================================================
export type CandidateKind = 'KANJI' | 'VOCAB' | 'GRAMMAR'

export interface MatchingCandidate {
  /**
   * Unique key dùng cho matching. Kết hợp prefix (K/V/G) với id gốc của item
   * trong DB để chắc chắn không trùng giữa các loại item.
   * VD: "K_42", "V_108", "G_15"
   */
  key: string

  /**
   * Nhãn chính hiển thị trong dropdown.
   * - Kanji:  chữ Hán (VD: 勉強)
   * - Vocab:  từ vựng (VD: 勉強する)
   * - Grammar: mẫu ngữ pháp (VD: 〜てもいい)
   */
  display: string

  /**
   * Phụ đề hiển thị bên dưới display.
   * - Kanji:  "Hán tự · nghĩa Vi"
   * - Vocab:  "Từ vựng · âm Hán Vi · nghĩa Vi"
   * - Grammar: "Ngữ pháp · nghĩa Vi"
   */
  hint: string

  kind: CandidateKind

  /**
   * ID gốc của item trong DB (kanjiItems.id / vocabularyItems.id / grammarItems.id).
   * Dùng để tracking nguồn, có thể click để xem chi tiết (future).
   */
  sourceId: number
}

const KIND_TAG_COLOR: Record<CandidateKind, string> = {
  KANJI: 'red',
  VOCAB: 'blue',
  GRAMMAR: 'purple',
}

const KIND_LABEL: Record<CandidateKind, string> = {
  KANJI: 'Hán tự',
  VOCAB: 'Từ vựng',
  GRAMMAR: 'Ngữ pháp',
}

/** Tag Antd color cho loại candidate — dùng trong AutoComplete option. */
export function getKindTagColor(kind: CandidateKind): string {
  return KIND_TAG_COLOR[kind]
}

/** Nhãn tiếng Việt cho loại candidate. */
export function getKindLabel(kind: CandidateKind): string {
  return KIND_LABEL[kind]
}

// ============================================================================
// Build candidates từ 1 lesson
// ============================================================================
/**
 * Trích tất cả item (Kanji + Vocab + Grammar) của 1 lesson → danh sách candidate.
 *
 * @param lesson - Lesson detail (đã có sẵn qua `useLessonDetail`)
 * @returns mảng candidate theo thứ tự: Kanji → Vocab → Grammar (theo từng section)
 */
export function buildCandidatesFromLesson(lesson: LessonDetail | null | undefined): MatchingCandidate[] {
  if (!lesson?.sections) return []

  const out: MatchingCandidate[] = []

  for (const section of lesson.sections) {
    // ---- KANJI ----
    for (const k of section.kanjiItems ?? []) {
      // Bỏ qua nếu không có character (data lỗi)
      if (!k.character) continue
      out.push({
        key: `K_${k.id}`,
        display: k.character,
        hint: `${KIND_LABEL.KANJI}${k.meaningVi ? ' · ' + k.meaningVi : ''}`,
        kind: 'KANJI',
        sourceId: k.id,
      })
    }

    // ---- VOCABULARY ----
    for (const v of section.vocabularyItems ?? []) {
      if (!v.word) continue
      // Ưu tiên displayMeaning > customMeaning > meaningVi
      const meaning = v.displayMeaning ?? v.customMeaning ?? v.meaningVi
      const reading = v.displayHiragana ?? v.hiragana
      const hintParts = [KIND_LABEL.VOCAB]
      if (reading) hintParts.push(reading)
      if (meaning) hintParts.push(meaning)
      out.push({
        key: `V_${v.id}`,
        display: v.word,
        hint: hintParts.join(' · '),
        kind: 'VOCAB',
        sourceId: v.id,
      })
    }

    // ---- GRAMMAR ----
    for (const g of section.grammarItems ?? []) {
      // Pattern có thể ở structure hoặc title
      const pattern = g.displayStructure ?? g.customStructure ?? g.structure ?? g.title
      if (!pattern) continue
      const meaning = g.displayMeaning ?? g.customMeaning ?? g.meaningVi
      const hintParts = [KIND_LABEL.GRAMMAR]
      if (meaning) hintParts.push(meaning)
      out.push({
        key: `G_${g.id}`,
        display: pattern,
        hint: hintParts.join(' · '),
        kind: 'GRAMMAR',
        sourceId: g.id,
      })
    }
  }

  return out
}

// ============================================================================
// Auto-pair candidates thành {left, right}
// ============================================================================
/**
 * Ghép candidate thành các cặp left/right cho câu hỏi MATCHING.
 *
 * <p><b>Quy ước key:</b> vế trái dùng {@code L1..Ln}, vế phải dùng
 * {@code R1..Rn} theo index gốc. MatchKey cho option sẽ là
 * {@code "L{i}=R{j}"} — {@code i} = index left trong matchingData, {@code j}
 * = index right của cặp đúng.</p>
 *
 * <p>Có 2 mode ghép (chọn qua {@code strategy}):</p>
 * <ul>
 *   <li><b>word-to-meaning</b> (mặc định): mỗi candidate → 1 cặp, left = display,
 *       right = phần nghĩa tách từ hint. Khi item không có nghĩa → fallback
 *       dùng display làm nghĩa để teacher tự sửa.</li>
 *   <li><b>alternating</b>: cứ 2 candidate liên tiếp → 1 cặp (lẻ = left, chẵn = right).</li>
 * </ul>
 *
 * @param candidates - danh sách candidate từ `buildCandidatesFromLesson`
 * @param strategy   - cách ghép (mặc định "word-to-meaning")
 * @returns {left, right} đúng shape matchingData BE yêu cầu
 */
export function autoPairCandidates(
  candidates: MatchingCandidate[],
  strategy: 'word-to-meaning' | 'alternating' = 'word-to-meaning',
): { left: MatchingSide[]; right: MatchingSide[] } {
  if (candidates.length < 2) return { left: [], right: [] }

  if (strategy === 'word-to-meaning') {
    // Mỗi candidate → 1 cặp: left = display, right = phần nghĩa (tách từ hint)
    const left: MatchingSide[] = []
    const right: MatchingSide[] = []
    candidates.forEach((c, i) => {
      // hint có dạng: "Hán tự · nghĩa Vi" → lấy phần sau dấu " · " đầu tiên
      const parts = c.hint.split(' · ').slice(1) // bỏ phần "Hán tự/Từ vựng/Ngữ pháp"
      const meaning = parts.join(' · ') || c.display // fallback nếu không có nghĩa
      // Key theo index gốc L1..Ln / R1..Rn (KHÔNG dùng K_42 / V_1_MEAN nữa)
      // để format matchKey = "L{i}=R{j}" được gọn.
      left.push({ key: `L${i + 1}`, label: c.display })
      right.push({ key: `R${i + 1}`, label: meaning })
    })
    return { left, right }
  }

  // strategy === 'alternating' — vẫn dùng key theo index gốc
  const left: MatchingSide[] = []
  const right: MatchingSide[] = []
  for (let i = 0; i + 1 < candidates.length; i += 2) {
    left.push({ key: `L${i + 1}`, label: candidates[i].display })
    right.push({ key: `R${i + 1}`, label: candidates[i + 1].display })
  }
  return { left, right }
}

/**
 * Tạo mảng right đã được shuffle (Fisher–Yates).
 *
 * <p>Trả về mảng MỚI (không mutate input). Đảm bảo 2 tính chất:</p>
 * <ul>
 *   <li>Hoán vị hợp lệ (không trùng, đủ N phần tử).</li>
 *   <li>Không trùng với identity ở mức xác suất rất thấp để học viên thực sự
 *       thấy thứ tự xáo (Fisher–Yates thuần sẽ ra identity với xác suất
 *       1/N! — đối với N>=3 thì cực thấp, nhưng ta reroll cho chắc).</li>
 * </ul>
 */
export function shuffleMatchingRight<T>(arr: T[]): T[] {
  const out = [...arr]
  if (out.length <= 1) return out
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[out[i], out[j]] = [out[j], out[i]]
  }
  // Reroll nếu trùng identity (xác suất rất thấp nhưng phòng trường hợp N=2)
  const isIdentity = out.every((v, i) => v === arr[i])
  if (isIdentity) out.reverse()
  return out
}

/**
 * Lọc candidate theo loại (dùng cho filter trong editor toolbar).
 */
export function filterCandidatesByKind(
  candidates: MatchingCandidate[],
  kind: 'ALL' | CandidateKind,
): MatchingCandidate[] {
  if (kind === 'ALL') return candidates
  return candidates.filter((c) => c.kind === kind)
}
