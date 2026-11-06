import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Spin, Tag, message } from 'antd'
import { BookOutlined, DownloadOutlined, CheckOutlined } from '@ant-design/icons'
import { notebookService } from '@/services/notebook.service'
import type { Notebook } from '@/types'
import { BackButton } from '@/components/common'

export const NotebookShareRedirectPage: React.FC = () => {
  const { shareCode } = useParams<{ shareCode: string }>()
  const navigate = useNavigate()

  const [notebook, setNotebook] = useState<Notebook | null>(null)
  const [loading, setLoading] = useState(true)
  const [cloning, setCloning] = useState(false)

  useEffect(() => {
    if (!shareCode) {
      navigate('/notebooks')
      return
    }

    const fetchShareNotebook = async () => {
      try {
        setLoading(true)
        const res = await notebookService.getNotebookByShareCode(shareCode)
        if (res.data) {
          setNotebook(res.data)
        }
      } catch {
        message.error('Không tìm thấy sổ tay hoặc liên kết chia sẻ đã hết hạn/bị tắt!')
      } finally {
        setLoading(false)
      }
    }

    fetchShareNotebook()
  }, [shareCode, navigate])

  const handleClone = async () => {
    if (!shareCode) return
    try {
      setCloning(true)
      const res = await notebookService.cloneNotebookByShareCode(shareCode)
      if (res.data) {
        message.success(`Đã nhân bản "${res.data.name}" vào sổ tay cá nhân của bạn!`)
        navigate(`/notebooks/${res.data.id}`)
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể nhân bản sổ tay này!'
      message.error(msg)
    } finally {
      setCloning(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-[400px] flex items-center justify-center">
        <Spin size="large" />
      </div>
    )
  }

  if (!notebook) {
    return (
      <div className="max-w-md mx-auto py-16 px-4 text-center space-y-4">
        <h2 className="text-xl font-bold text-gray-800">Không tìm thấy sổ tay!</h2>
        <p className="text-sm text-gray-500">
          Mã chia sẻ "{shareCode}" không tồn tại hoặc chủ sở hữu đã tắt tính năng chia sẻ.
        </p>
        <Button onClick={() => navigate('/notebooks')}>
          Quay lại danh sách sổ tay
        </Button>
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto py-12 px-4 space-y-6">
      <BackButton to="/notebooks" />

      <div className="bg-surface rounded-3xl border border-gray-200 p-8 shadow-sm text-center space-y-4">
        <div className="w-16 h-16 mx-auto bg-blue-50 rounded-2xl flex items-center justify-center text-blue-600 border border-blue-100">
          <BookOutlined className="text-3xl" />
        </div>

        <div className="space-y-1">
          <div className="flex items-center justify-center gap-2">
            <h1 className="text-2xl font-bold text-gray-900">{notebook.name}</h1>
            {notebook.jlptLevel && <Tag color="blue">{notebook.jlptLevel}</Tag>}
          </div>
          <div className="text-xs text-gray-500">
            Tác giả: <span className="font-semibold text-gray-700">{notebook.authorName || 'Ẩn danh'}</span> • {notebook.wordCount} từ vựng
          </div>
          {notebook.description && (
            <div className="text-xs text-gray-600 italic bg-gray-50 p-3 rounded-xl border border-gray-100 max-w-md mx-auto mt-2">
              "{notebook.description}"
            </div>
          )}
        </div>

        <div className="pt-4">
          {notebook.isOwner ? (
            <div className="space-y-3">
              <div className="p-3 bg-purple-50 text-purple-700 rounded-xl text-sm font-medium border border-purple-100">
                Bạn là tác giả và sổ tay này đã có sẵn trong kho cá nhân của bạn!
              </div>
              <Button
                type="primary"
                size="large"
                onClick={() => navigate(`/notebooks/${notebook.id}`)}
                className="w-full h-12 bg-purple-600 hover:bg-purple-700 font-semibold rounded-xl text-base shadow-xs"
              >
                Mở sổ tay của bạn
              </Button>
            </div>
          ) : notebook.isImported ? (
            <div className="space-y-3">
              <div className="p-3 bg-emerald-50 text-emerald-700 rounded-xl text-sm font-medium border border-emerald-100 flex items-center justify-center gap-1.5">
                <CheckOutlined /> Bạn đã lưu sổ tay này vào kho cá nhân rồi!
              </div>
              <Button
                type="primary"
                size="large"
                icon={<CheckOutlined />}
                onClick={() => navigate(`/notebooks/${notebook.userNotebookId || notebook.id}`)}
                className="w-full h-12 bg-emerald-600 hover:bg-emerald-700 font-semibold rounded-xl text-base shadow-xs"
              >
                Đã có trong kho - Mở ngay
              </Button>
            </div>
          ) : (
            <Button
              type="primary"
              size="large"
              icon={<DownloadOutlined />}
              loading={cloning}
              onClick={handleClone}
              className="w-full h-12 bg-blue-600 hover:bg-blue-700 font-semibold rounded-xl text-base shadow-xs"
            >
              Nhân bản vào kho sổ tay của tôi
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
export default NotebookShareRedirectPage

