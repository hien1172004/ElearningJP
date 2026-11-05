import type { CSSProperties } from 'react'

/**
 * Thanh tab (antd Tabs) dạng thẻ nền trắng để không bị chìm trên nền trang (kinari).
 * Dùng: <Tabs tabBarStyle={WHITE_TAB_BAR_STYLE} className={WHITE_TAB_BAR_CLASS} ... />
 */
export const WHITE_TAB_BAR_STYLE: CSSProperties = {
  // Co theo nội dung các tab thay vì kéo hết chiều ngang
  width: 'fit-content',
  maxWidth: '100%',
  background: '#ffffff',
  padding: '0 16px',
  borderRadius: 12,
  border: '1px solid var(--color-suna)',
  boxShadow: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
}

/** Ẩn đường kẻ đáy mặc định của antd để không bị trùng với viền thẻ */
export const WHITE_TAB_BAR_CLASS = '[&>.ant-tabs-nav]:before:hidden!'
