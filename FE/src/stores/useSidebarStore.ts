import { create } from 'zustand'

/**
 * Quản lý trạng thái sidebar (collapsed/expanded) + drawer mobile.
 *
 * - `collapsed`: dùng cho desktop, true = chỉ hiện icon, false = hiện icon + label.
 * - `mobileOpen`: dùng cho mobile drawer, true = đang mở, false = đang đóng.
 */

interface SidebarState {
  collapsed: boolean
  mobileOpen: boolean
  toggle: () => void
  setCollapsed: (value: boolean) => void
  setMobileOpen: (value: boolean) => void
  closeMobile: () => void
}

export const useSidebarStore = create<SidebarState>((set) => ({
  // Mặc định: mở rộng (theo lựa chọn của người dùng)
  collapsed: false,
  mobileOpen: false,
  toggle: () => set((s) => ({ collapsed: !s.collapsed })),
  setCollapsed: (value) => set({ collapsed: value }),
  setMobileOpen: (value) => set({ mobileOpen: value }),
  closeMobile: () => set({ mobileOpen: false }),
}))
