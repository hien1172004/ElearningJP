export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('vi-VN').format(num)
}

export const formatDate = (date: string | Date): string => {
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(date))
}
