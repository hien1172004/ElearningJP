# ElearningJP - Frontend Client

Dự án Frontend cho Hệ thống học tiếng Nhật trực tuyến (ElearningJP), được xây dựng bằng **React 19**, **TypeScript**, **Vite** và **Tailwind CSS**.

---

## 📁 Cấu trúc thư mục (`src/`)

```text
src/
├── assets/          # Chứa tài nguyên tĩnh: hình ảnh, logo, icons, audio mẫu,...
├── components/      # Các UI component tái sử dụng trong toàn bộ ứng dụng
│   ├── common/      # Component cơ bản: Button, Input, Modal, Badge, Dropdown,...
│   └── feedback/    # Component trạng thái: LoadingSpinner, EmptyState, Alert,...
├── layouts/         # Các khung bố cục trang dùng chung
│   ├── AuthLayout   # Bố cục cho trang Đăng nhập / Đăng ký
│   ├── MainLayout   # Bố cục chính cho Học viên (Header, Navigation, Footer)
│   └── AdminLayout  # Bố cục cho Quản trị viên
├── pages/           # Chứa các trang (màn hình) của ứng dụng
│   ├── auth/        # Login, Register, ForgotPassword
│   ├── home/        # Trang chủ giới thiệu
│   ├── dashboard/   # Trang tổng quan tiến độ học tập
│   ├── courses/     # Danh sách khóa học & bài học
│   ├── srs/         # Ôn tập Flashcard ngắt quãng
│   └── exam/        # Luyện thi thử JLPT
├── routes/          # Cấu hình định tuyến (React Router) & ProtectedRoute
├── services/        # Tầng kết nối & gọi API Backend Spring Boot
│   ├── api/         # Cấu hình Axios instance (Base URL, JWT Interceptors)
│   ├── auth.service # API Đăng nhập, Đăng ký, Refresh token
│   └── user.service # API Thông tin tài khoản
├── stores/          # Quản lý State toàn cục bằng Zustand (useAuthStore)
├── types/           # Định nghĩa kiểu dữ liệu TypeScript (User, ApiResponse,...)
└── utils/           # Các hàm tiện ích dùng chung (storage, format ngày/số,...)
```

---

## 🔄 Luồng làm việc chuẩn khi thêm tính năng mới

Khi xây dựng một tính năng mới (ví dụ: *Xem danh sách khóa học*), luồng code khuyến nghị:
1. **Định nghĩa kiểu dữ liệu** (`src/types/course.types.ts`).
2. **Viết hàm gọi API** (`src/services/course.service.ts`).
3. **Quản lý state** nếu cần lưu toàn cục (`src/stores/useCourseStore.ts`).
4. **Tạo giao diện trang & component** (`src/pages/courses/`, `src/components/`).
5. **Khai báo đường dẫn** trong `src/routes/index.tsx`.

---

## 🛠️ Thư viện chính sử dụng

- **Giao diện**: [Tailwind CSS v4](https://tailwindcss.com), [Lucide React Icons](https://lucide.dev)
- **Routing**: [React Router](https://reactrouter.com)
- **HTTP Client**: [Axios](https://axios-http.com)
- **Data Caching**: [@tanstack/react-query](https://tanstack.com/query)
- **State Management**: [Zustand](https://zustand-demo.pmnd.rs)
- **Form & Validation**: [React Hook Form](https://react-hook-form.com) & [Zod](https://zod.dev)
- **Thông báo**: [Sonner](https://sonner.emilkowal.ski)

---

## 🚀 Hướng dẫn chạy dự án

```bash
# 1. Cài đặt thư viện (nếu mới clone về)
npm install

# 2. Khởi chạy môi trường phát triển (Dev server)
npm run dev

# 3. Kiểm tra kiểu & đóng gói sản phẩm (Build production)
npm run build
```
