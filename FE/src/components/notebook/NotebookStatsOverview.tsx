import React from 'react'
import { Info, Play } from 'lucide-react'
import { Switch, Tooltip } from 'antd'

interface NotebookStatsOverviewProps {
  unlearnedCount?: number
  barelyCount?: number
  moderateCount?: number
  masteredCount?: number
  srEnabled?: boolean
  onToggleSr?: (checked: boolean) => void
  onStartReview?: () => void
}

export const NotebookStatsOverview: React.FC<NotebookStatsOverviewProps> = ({
  unlearnedCount = 0,
  barelyCount = 0,
  moderateCount = 0,
  masteredCount = 0,
  srEnabled = false,
  onToggleSr,
  onStartReview,
}) => {
  return (
    <div className="bg-surface border border-gray-100 rounded-3xl p-5 shadow-xs mb-6">
      {/* Top: Header + Toggle SR + Start Review Button */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <h2 className="text-base font-bold text-gray-800">Tổng quan</h2>
          <div className="h-4 w-px bg-gray-200" />
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <span>Học lặp lại ngắt quãng</span>
            <Tooltip title="Thuật toán SM-2 giúp ghi nhớ lâu bằng cách nhắc nhở ôn từ vào đúng thời điểm bạn chuẩn bị quên.">
              <Info className="w-4 h-4 text-gray-400 cursor-pointer hover:text-gray-600" />
            </Tooltip>
            <Switch
              checked={srEnabled}
              onChange={onToggleSr}
              className="bg-gray-200"
            />
          </div>
        </div>

        <button
          type="button"
          onClick={onStartReview}
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm px-5 py-2 rounded-xl shadow-xs transition-colors"
        >
          <Play className="w-4 h-4 fill-white" />
          <span>Ôn tập sổ tay</span>
        </button>
      </div>

      {/* 4 Cards: Chưa thuộc / Thuộc lơ mơ / Thuộc kha khá / Thuộc làu làu */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {/* Chưa thuộc */}
        <div className="bg-surface border border-red-200 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-1.5 h-10 rounded-full bg-red-500" />
          <div>
            <div className="text-xs text-gray-500 font-medium">Chưa thuộc</div>
            <div className="text-xl font-bold text-gray-800 mt-0.5">
              {unlearnedCount} <span className="text-xs font-normal text-gray-400">từ</span>
            </div>
          </div>
        </div>

        {/* Thuộc lơ mơ */}
        <div className="bg-surface border border-amber-200 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-1.5 h-10 rounded-full bg-amber-500" />
          <div>
            <div className="text-xs text-gray-500 font-medium">Thuộc lơ mơ</div>
            <div className="text-xl font-bold text-gray-800 mt-0.5">
              {barelyCount} <span className="text-xs font-normal text-gray-400">từ</span>
            </div>
          </div>
        </div>

        {/* Thuộc kha khá */}
        <div className="bg-surface border border-blue-200 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-1.5 h-10 rounded-full bg-blue-500" />
          <div>
            <div className="text-xs text-gray-500 font-medium">Thuộc kha khá</div>
            <div className="text-xl font-bold text-gray-800 mt-0.5">
              {moderateCount} <span className="text-xs font-normal text-gray-400">từ</span>
            </div>
          </div>
        </div>

        {/* Thuộc làu làu */}
        <div className="bg-surface border border-green-200 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-1.5 h-10 rounded-full bg-green-500" />
          <div>
            <div className="text-xs text-gray-500 font-medium">Thuộc làu làu</div>
            <div className="text-xl font-bold text-gray-800 mt-0.5">
              {masteredCount} <span className="text-xs font-normal text-gray-400">từ</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

