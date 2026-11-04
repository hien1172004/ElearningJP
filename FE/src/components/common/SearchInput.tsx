import React, { useEffect, useRef } from 'react'
import { Input } from 'antd'
import { Search } from 'lucide-react'
import { cn } from '@/utils/cn'

interface SearchInputProps {
  value: string
  onChange: (value: string) => void
  /** Gọi với từ khóa đã trim: sau khi ngừng gõ `debounceMs`, hoặc ngay khi nhấn Enter / xóa trắng. */
  onSearch: (keyword: string) => void
  placeholder?: string
  className?: string
  debounceMs?: number
}

/**
 * Ô tìm kiếm cho các trang quản trị: icon kính lúp ở đầu ô (khớp chiều cao input của theme),
 * tự tìm khi ngừng gõ, Enter để tìm ngay — không cần nút bấm riêng.
 */
export const SearchInput: React.FC<SearchInputProps> = ({
  value,
  onChange,
  onSearch,
  placeholder,
  className,
  debounceMs = 400,
}) => {
  const timerRef = useRef<number | undefined>(undefined)

  // Dọn timer khi rời trang
  useEffect(() => () => window.clearTimeout(timerRef.current), [])

  const searchNow = (v: string) => {
    window.clearTimeout(timerRef.current)
    onSearch(v.trim())
  }

  const handleChange = (v: string) => {
    onChange(v)
    window.clearTimeout(timerRef.current)
    if (!v) {
      onSearch('') // bấm nút xóa → bỏ lọc ngay
      return
    }
    timerRef.current = window.setTimeout(() => onSearch(v.trim()), debounceMs)
  }

  return (
    <Input
      allowClear
      value={value}
      placeholder={placeholder}
      prefix={<Search className="w-4 h-4 text-slate-400" aria-hidden />}
      onChange={(e) => handleChange(e.target.value)}
      onPressEnter={(e) => searchNow(e.currentTarget.value)}
      className={cn(className)}
      aria-label={placeholder ?? 'Tìm kiếm'}
    />
  )
}
