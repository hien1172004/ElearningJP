import React, { useState } from 'react'
import {
  Table,
  Button,
  Space,
  Tag,
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
  { label: string; color: string; bg: string }
> = {
  NOT_LEARNED: { label: 'Chưa học', color: '#8c8c8c', bg: '#f5f5f5' },
  BARELY: { label: 'Mới học', color: '#ff4d4f', bg: '#fff1f0' },
  MODERATE: { label: 'Nhớ tạm', color: '#faad14', bg: '#fffbe6' },
  MASTERED: { label: 'Đã thuộc', color: '#52c41a', bg: '#f6ffed' },
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
      title: 'Từ vựng',
      dataIndex: 'word',
      key: 'word',
      width: 220,
      render: (_, record) => (
        <div className="flex items-center gap-2">
          <Button
            type="text"
            shape="circle"
            size="small"
            icon={<SoundOutlined className="text-blue-500" />}
            onClick={() => playAudio(record)}
            aria-label="Phát âm"
          />
          <div>
            <div className="font-bold text-base text-gray-800 leading-tight">
              {record.word}
            </div>
            {record.reading && (
              <div className="text-xs text-gray-500">{record.reading}</div>
            )}
            {(record.sinoVietnamese || record.hanViet) && (
              <div className="text-[11px] text-emerald-600 font-medium">
                [{record.sinoVietnamese || record.hanViet}]
              </div>
            )}
          </div>
        </div>
      ),
    },
    {
      title: 'Ý nghĩa',
      dataIndex: 'meaning',
      key: 'meaning',
      ellipsis: true,
      render: (meaning: string, record) => (
        <div>
          <div className="text-sm font-medium text-gray-700">{meaning || '—'}</div>
          {record.partOfSpeech && (
            <span className="text-[11px] text-gray-400 italic">
              ({record.partOfSpeech})
            </span>
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
              />
              <Button
                size="small"
                type="primary"
                icon={<CheckOutlined />}
                onClick={() => handleSaveNote(record.id)}
              />
            </div>
          )
        }
        return (
          <div
            className="group/note flex items-center justify-between text-xs text-gray-500 hover:text-gray-800 cursor-pointer p-1 rounded hover:bg-gray-50"
            onClick={() => handleStartEditNote(record)}
          >
            <span className="line-clamp-1 italic">
              {note || '+ Thêm ghi chú của bạn ở đây'}
            </span>
            <EditOutlined className="opacity-0 group-hover/note:opacity-100 text-gray-400 ml-1 text-[11px]" />
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
          <Tag
            color={info.bg}
            style={{ color: info.color, borderColor: info.color }}
            className="rounded-full px-2 py-0.5 text-xs font-medium"
          >
            {info.label}
          </Tag>
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
              icon={<EditOutlined className="text-gray-500 hover:text-blue-600" />}
              onClick={() => onEditWord(record)}
            />
          </Tooltip>
          <Tooltip title="Xóa khỏi sổ tay">
            <Popconfirm
              title="Xác nhận xóa"
              description="Bạn có chắc chắn muốn xóa từ này khỏi sổ tay?"
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
    <div className="bg-white rounded-xl shadow-xs border border-suna overflow-hidden">
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
          showTotal: (totalCount) => `Tổng cộng ${totalCount} từ`,
        }}
        locale={{
          emptyText: (
            <div className="py-12 text-center text-gray-400">
              <BookOutlined className="text-4xl mb-2 text-gray-300" />
              <div>Sổ tay này chưa có từ vựng nào.</div>
              <div className="text-xs text-gray-400 mt-1">
                Bấm nút "+ Thêm từ" hoặc "Nhập Excel" để bắt đầu học nhé!
              </div>
            </div>
          ),
        }}
      />
    </div>
  )
}
export default WordTable
