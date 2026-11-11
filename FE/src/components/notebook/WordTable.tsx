import React, { useState } from 'react'
import {
  Table,
  Button,
  Space,
  Popconfirm,
  Input,
  Tooltip,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  SoundOutlined,
  EditOutlined,
  DeleteOutlined,
  BookOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import type { NotebookWord, MemoryLevel } from '@/types'
import { NOTEBOOK_ITEM_TYPE_META, itemTypeOf } from './itemTypeMeta'

interface WordTableProps {
  words: NotebookWord[]
  loading?: boolean
  total: number
  currentPage: number
  pageSize: number
  selectedWordIds: number[]
  onPageChange: (page: number, pageSize: number) => void
  onSelectionChange: (selectedKeys: number[]) => void
  onEditWord: (word: NotebookWord) => void
  onDeleteWord: (wordId: number) => void
  onUpdateNote?: (wordId: number, note: string) => void
}

const MEMORY_LEVEL_MAP: Record<
  MemoryLevel,
  { label: string; color: string; bg: string; border: string }
> = {
  NOT_LEARNED: { label: 'Chưa học', color: '#6b7280', bg: '#f9fafb', border: '#e5e7eb' },
  BARELY: { label: 'Mới học', color: '#c53030', bg: '#fff5f5', border: '#fed7d7' },
  MODERATE: { label: 'Nhớ tạm', color: '#b45309', bg: '#fffbeb', border: '#fde68a' },
  MASTERED: { label: 'Đã thuộc', color: '#15803d', bg: '#f0fdf4', border: '#bbf7d0' },
}

export const WordTable: React.FC<WordTableProps> = ({
  words,
  loading = false,
  total,
  currentPage,
  pageSize,
  selectedWordIds,
  onPageChange,
  onSelectionChange,
  onEditWord,
  onDeleteWord,
  onUpdateNote,
}) => {
  const [editingNoteId, setEditingNoteId] = useState<number | null>(null)
  const [tempNote, setTempNote] = useState<string>('')

  const playAudio = (word: NotebookWord) => {
    if (word.audioUrl) {
      const audio = new Audio(word.audioUrl)
      audio.play().catch(() => playSpeech(word.word))
    } else {
      playSpeech(word.word)
    }
  }

  const playSpeech = (text: string) => {
    if (!('speechSynthesis' in window)) return
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'ja-JP'
    utterance.rate = 0.9
    window.speechSynthesis.speak(utterance)
  }

  const handleStartEditNote = (word: NotebookWord) => {
    setEditingNoteId(word.id)
    setTempNote(word.userNote || '')
  }

  const handleSaveNote = (wordId: number) => {
    if (onUpdateNote) {
      onUpdateNote(wordId, tempNote)
    }
    setEditingNoteId(null)
  }

  const columns: ColumnsType<NotebookWord> = [
    {
      title: 'Mục',
      dataIndex: 'word',
      key: 'word',
      width: 240,
      render: (_, record) => {
        const type = itemTypeOf(record)
        const meta = NOTEBOOK_ITEM_TYPE_META[type]
        return (
          <div className="flex items-center gap-2">
            <Button
              type="text"
              shape="circle"
              size="small"
              icon={<SoundOutlined className="text-ai hover:text-ai/80" />}
              onClick={() => playAudio(record)}
              aria-label="Phát âm"
            />
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <span
                  className={`font-bold text-sumi leading-tight font-sans ${type === 'KANJI' ? 'text-2xl' : 'text-base'}`}
                >
                  {record.word}
                </span>
                <span className={`inline-flex items-center text-[10px] font-medium px-1.5 py-0.5 rounded border leading-none ${meta.chipClass}`}>
                  {meta.label}
                </span>
              </div>
              {type === 'KANJI' ? (
                <div className="text-xs text-nibi space-y-0.5">
                  {record.onyomi && record.onyomi.length > 0 && <div>On: {record.onyomi.join('、')}</div>}
                  {record.kunyomi && record.kunyomi.length > 0 && <div>Kun: {record.kunyomi.join('、')}</div>}
                  {!record.onyomi?.length && !record.kunyomi?.length && record.reading && <div>{record.reading}</div>}
                  {record.strokeCount != null && <div className="text-nibi/70">{record.strokeCount} nét</div>}
                </div>
              ) : type === 'GRAMMAR' ? (
                (record.reading || record.grammarStructure) && (
                  <div className="text-xs text-shu font-mono break-words">
                    {record.reading || record.grammarStructure}
                  </div>
                )
              ) : (
                record.reading && <div className="text-xs text-nibi">{record.reading}</div>
              )}
              {type !== 'GRAMMAR' && (record.sinoVietnamese || record.hanViet) && (
                <div className="text-[11px] text-ai font-medium">
                  [{record.sinoVietnamese || record.hanViet}]
                </div>
              )}
            </div>
          </div>
        )
      },
    },
    {
      title: 'Ý nghĩa',
      dataIndex: 'meaning',
      key: 'meaning',
      ellipsis: true,
      render: (meaning: string, record) => (
        <div>
          <div className="text-sm font-medium text-sumi">{meaning || '—'}</div>
          {record.partOfSpeech && itemTypeOf(record) === 'VOCAB' && (
            <span className="text-[11px] text-nibi italic">
              ({record.partOfSpeech})
            </span>
          )}
          {record.exampleSentence && (
            <div className="text-[11px] text-nibi mt-0.5 line-clamp-1" title={record.exampleMeaning ?? undefined}>
              VD: {record.exampleSentence}
            </div>
          )}
          {record.sourceLessonTitle && (
            <div className="text-[11px] text-ai mt-0.5 line-clamp-1">Từ bài: {record.sourceLessonTitle}</div>
          )}
        </div>
      ),
    },
    {
      title: 'Ghi chú của bạn',
      dataIndex: 'userNote',
      key: 'userNote',
      width: 260,
      render: (note: string, record) => {
        if (editingNoteId === record.id) {
          return (
            <div className="flex items-center gap-1">
              <Input
                size="small"
                value={tempNote}
                onChange={(e) => setTempNote(e.target.value)}
                onPressEnter={() => handleSaveNote(record.id)}
                placeholder="Nhập ghi chú..."
                autoFocus
                className="rounded-lg border-suna text-xs"
              />
              <Button
                size="small"
                type="primary"
                icon={<CheckOutlined />}
                onClick={() => handleSaveNote(record.id)}
                className="rounded-lg bg-ai"
              />
            </div>
          )
        }
        return (
          <div
            className="group/note flex items-center justify-between text-xs text-nibi hover:text-sumi cursor-pointer p-1.5 rounded-lg hover:bg-slate-50 transition-colors"
            onClick={() => handleStartEditNote(record)}
          >
            {note ? (
              <span className="line-clamp-1 text-sumi">{note}</span>
            ) : (
              <span className="text-nibi/40 group-hover/note:text-ai italic flex items-center gap-1 text-[11px] transition-colors">
                <EditOutlined className="text-[10px]" /> Thêm ghi chú
              </span>
            )}
            <EditOutlined className="opacity-0 group-hover/note:opacity-100 text-nibi ml-1 text-[11px] transition-opacity" />
          </div>
        )
      },
    },
    {
      title: 'Mức ghi nhớ',
      dataIndex: 'memoryLevel',
      key: 'memoryLevel',
      width: 130,
      render: (level: MemoryLevel) => {
        const info = MEMORY_LEVEL_MAP[level] || MEMORY_LEVEL_MAP.NOT_LEARNED
        return (
          <span
            style={{ color: info.color, backgroundColor: info.bg, borderColor: info.border }}
            className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border"
          >
            {info.label}
          </span>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Chỉnh sửa từ vựng">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined className="text-nibi hover:text-ai" />}
              onClick={() => onEditWord(record)}
            />
          </Tooltip>
          <Tooltip title="Xóa khỏi sổ tay">
            <Popconfirm
              title="Xác nhận xóa"
              description="Bạn có chắc chắn muốn xóa mục này khỏi sổ tay?"
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true }}
              onConfirm={() => onDeleteWord(record.id)}
            >
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
              />
            </Popconfirm>
          </Tooltip>
        </Space>
      ),
    },
  ]

  const rowSelection = {
    selectedRowKeys: selectedWordIds,
    onChange: (keys: React.Key[]) => {
      onSelectionChange(keys as number[])
    },
  }

  return (
    <div className="rounded-xl overflow-hidden border border-suna/80">
      <Table<NotebookWord>
        rowKey="id"
        rowSelection={rowSelection}
        columns={columns}
        dataSource={words}
        loading={loading}
        pagination={{
          current: currentPage + 1,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          onChange: (page, pSize) => onPageChange(page - 1, pSize),
          showTotal: (totalCount) => `Tổng cộng ${totalCount} mục`,
        }}
        locale={{
          emptyText: (
            <div className="py-12 text-center text-nibi">
              <BookOutlined className="text-4xl mb-2 text-nibi/40" />
              <div className="font-medium text-sumi">Chưa có mục nào trong danh sách</div>
              <div className="text-xs text-nibi mt-1">
                Bấm "+ Thêm từ", "Nhập Excel", hoặc lưu từ bài học / từ điển để bắt đầu học nhé!
              </div>
            </div>
          ),
        }}
      />
    </div>
  )
}
export default WordTable
