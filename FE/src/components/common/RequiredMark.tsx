import React from 'react'

/**
 * Dấu * đỏ sau nhãn trường bắt buộc. Form antd tự gắn (ConfigProvider `form.requiredMark` ở AntdProvider);
 * label tự viết (`<label>`, tiêu đề khối) thì đặt component này ngay sau chữ.
 */
export const RequiredMark: React.FC = () => (
  <span aria-hidden className="ml-0.5 font-semibold text-red-500">
    *
  </span>
)
