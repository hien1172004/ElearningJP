import React, { useMemo } from 'react'
import { ConfigProvider, theme as antdTheme } from 'antd'
import { cn } from '@/utils/cn'

interface FilterBarProps {
  children: React.ReactNode
  className?: string
}

/**
 * Khối "tìm kiếm + bộ lọc" của các trang quản trị (theo thiết kế trang Quản lý đề thi):
 * khung nền trắng, các ô Input / Select bên trong dùng màu nền trang (kinari), khi focus chuyển sang nền trắng.
 * Màu lấy từ token antd của chế độ sáng / tối hiện tại nên tự đổi theo dark mode.
 */
export const FilterBar: React.FC<FilterBarProps> = ({ children, className }) => {
  const { token } = antdTheme.useToken()
  const filterTheme = useMemo(
    () => ({
      token: { colorBgContainer: token.colorBgLayout },
      // Chỉ ghi đè nền; antd gộp với cấu hình Input chung (chiều cao, bo góc) của AntdProvider
      components: { Input: { activeBg: token.colorBgContainer } },
    }),
    [token.colorBgLayout, token.colorBgContainer],
  )

  return (
    <ConfigProvider theme={filterTheme}>
      <div
        className={cn(
          'bg-surface p-4 rounded-2xl border border-slate-200/80 shadow-xs flex flex-col md:flex-row md:flex-wrap gap-3',
          className,
        )}
      >
        {children}
      </div>
    </ConfigProvider>
  )
}
