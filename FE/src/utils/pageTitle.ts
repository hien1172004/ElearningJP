/**
 * Tiêu đề trang động cho ElearningJP theo từng đường dẫn (pathname)
 */
export function getPageTitle(pathname: string): string {
  // Chuẩn hóa path
  const path = pathname.replace(/\/+$/, '') || '/'

  // 1. Khớp chính xác các trang tĩnh
  const STATIC_TITLES: Record<string, string> = {
    '/': 'Trang chủ | ElearningJP',
    '/dictionary': 'Tra từ điển & Chữ cái | ElearningJP',
    '/courses': 'Khóa học tiếng Nhật | ElearningJP',
    '/exams': 'Thi thử JLPT | ElearningJP',
    '/exam-history': 'Lịch sử thi | ElearningJP',
    '/speaking': 'Luyện hội thoại Kaiwa | ElearningJP',
    '/speaking/free': 'Hội thoại tự do với AI | ElearningJP',
    '/progress': 'Tiến độ học tập | ElearningJP',
    '/progress/goal': 'Mục tiêu học tập | ElearningJP',
    '/srs': 'Ôn tập Spaced Repetition (SRS) | ElearningJP',
    '/notebooks': 'Sổ tay từ vựng | ElearningJP',
    '/notebooks/explore': 'Khám phá sổ tay cộng đồng | ElearningJP',
    '/profile': 'Tài khoản cá nhân | ElearningJP',
    '/login': 'Đăng nhập | ElearningJP',
    '/register': 'Đăng ký tài khoản | ElearningJP',
    '/forgot-password': 'Quên mật khẩu | ElearningJP',
    '/reset-password': 'Đặt lại mật khẩu | ElearningJP',

    // Quản lý (Admin / Teacher)
    '/admin/exams': 'Quản lý đề thi | ElearningJP',
    '/admin/exams/create': 'Tạo đề thi mới | ElearningJP',
    '/admin/scenarios': 'Kịch bản luyện nói | ElearningJP',
    '/admin/scenarios/new': 'Tạo kịch bản nói mới | ElearningJP',
    '/admin/courses': 'Quản lý khóa học | ElearningJP',
    '/admin/users': 'Quản lý người dùng | ElearningJP',
    '/admin/stats': 'Thống kê hệ thống | ElearningJP',
    '/admin/comments': 'Kiểm duyệt bình luận | ElearningJP',
    '/admin/broadcasts': 'Gửi thông báo | ElearningJP',
    '/admin/course-reviews': 'Hàng chờ duyệt khóa học | ElearningJP',
    '/teacher/courses': 'Quản lý khóa học | ElearningJP',
    '/teacher/students': 'Học viên của tôi | ElearningJP',
  }

  if (STATIC_TITLES[path]) {
    return STATIC_TITLES[path]
  }

  // 2. Khớp các trang động (dynamic routes)
  if (path.includes('/taking/')) return 'Phòng thi JLPT | ElearningJP'
  if (path.startsWith('/exams/result/')) return 'Kết quả thi | ElearningJP'
  if (path.startsWith('/exams/review/')) return 'Xem lại bài thi | ElearningJP'
  if (path.startsWith('/courses/') && path.includes('/lessons/')) return 'Học bài | ElearningJP'
  if (path.startsWith('/courses/')) return 'Chi tiết khóa học | ElearningJP'
  if (path.startsWith('/notebooks/') && path.includes('/review')) return 'Ôn tập sổ tay | ElearningJP'
  if (path.startsWith('/notebooks/')) return 'Chi tiết sổ tay | ElearningJP'
  if (path.startsWith('/speaking/scenario/')) return 'Kịch bản hội thoại | ElearningJP'
  if (path.startsWith('/speaking/session/')) return 'Phòng luyện nói AI | ElearningJP'
  if (path.startsWith('/admin/scenarios/')) return 'Chỉnh sửa kịch bản | ElearningJP'
  if (path.startsWith('/admin/courses/')) return 'Biên soạn khóa học | ElearningJP'

  return 'ElearningJP - Nền tảng học tiếng Nhật thông minh'
}

