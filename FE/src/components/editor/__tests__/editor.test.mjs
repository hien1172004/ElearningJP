import test from 'node:test'
import assert from 'node:assert/strict'
import DOMPurify from 'dompurify'
import { JSDOM } from 'jsdom'
import Kuroshiro from 'kuroshiro'
import KuromojiAnalyzer from 'kuroshiro-analyzer-kuromoji'

// Initialize DOMPurify in Node environment
const window = new JSDOM('').window
const purify = DOMPurify(window)

const ALLOWED_TAGS = ['p', 'br', 'strong', 'b', 'u', 'ruby', 'rt', 'span']
const ALLOWED_ATTR = ['data-speaker', 'class']

function sanitizeJapaneseHtml(dirtyHtml) {
  if (!dirtyHtml) return ''
  return purify.sanitize(dirtyHtml, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    KEEP_CONTENT: true,
  })
}

function normalizeLegacy(raw) {
  if (!raw) return '<p></p>'
  const trimmed = raw.trim()
  if (trimmed.startsWith('<p') || trimmed.startsWith('<div')) {
    return trimmed
  }
  const lines = trimmed.split(/\r?\n/)
  const paragraphs = lines.map((line) => `<p>${line || '<br>'}</p>`).join('')
  return paragraphs || '<p></p>'
}

test('1. Sanitize HTML - strips dangerous script and attributes', () => {
  const dirty = '<p>こんにちは <script>alert("xss")</script><img src="x" onerror="alert(1)"/><strong>太字</strong></p>'
  const clean = sanitizeJapaneseHtml(dirty)
  assert.ok(!clean.includes('<script>'), 'Must strip script tags')
  assert.ok(!clean.includes('onerror'), 'Must strip inline event handlers')
  assert.ok(clean.includes('<strong>太字</strong>'), 'Must retain allowed strong tag')
})

test('2. Sanitize HTML - preserves ruby, rt, and speaker badge', () => {
  const html = '<p><span data-speaker="男">男：</span><ruby>体育館<rt>たいいくかん</rt></ruby>へ行こう。<u>重要</u></p>'
  const clean = sanitizeJapaneseHtml(html)
  assert.ok(clean.includes('<ruby>体育館<rt>たいいくかん</rt></ruby>'), 'Must retain ruby tag')
  assert.ok(clean.includes('data-speaker="男"'), 'Must retain data-speaker attribute')
  assert.ok(clean.includes('<u>重要</u>'), 'Must retain underline tag')
})

test('3. normalizeLegacy - wraps plain multiline text into paragraphs', () => {
  const legacy = '男：明日の天気はどう？\n女：雨が降るそうです。'
  const normalized = normalizeLegacy(legacy)
  assert.strictEqual(
    normalized,
    '<p>男：明日の天気はどう？</p><p>女：雨が降るそうです。</p>',
    'Must wrap raw lines in <p> tags'
  )
})

test('4. Kuroshiro + Kuromoji - client analyzer converts Kanji to Hiragana', async () => {
  const kuroshiro = new Kuroshiro()
  await kuroshiro.init(new KuromojiAnalyzer({ dictPath: './public/dict' }))
  const reading = await kuroshiro.convert('体育館', { to: 'hiragana' })
  assert.strictEqual(reading, 'たいいくかん', 'Kuroshiro should accurately convert 体育館 to たいいくかん')
})
