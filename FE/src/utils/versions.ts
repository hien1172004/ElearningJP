/**
 * Map id → version gửi kèm khi sắp xếp lại hàng loạt (optimistic locking):
 * người khác vừa sắp xếp / sửa phần tử → BE trả 409 thay vì ghi đè thứ tự.
 * Phần tử chưa có version (dữ liệu cũ) được bỏ qua.
 */
export const toVersionMap = (items: ReadonlyArray<{ id: number; version?: number | null }>): Record<number, number> =>
  Object.fromEntries(items.filter((i) => i.version != null).map((i) => [i.id, i.version as number]))
