# Module 9 Implementation Guide — 3-Stage B2B Onboarding Portal & Auth Pages

> **Target Audience**: Mid-to-Senior Fullstack Engineers & Security Architects  
> **Module Scope**: B2B Registration with Stepper tracking, Role-based intelligent login redirection, Stage 1 review / Re-apply flow, Stage 2 admin-approval confirmation, Tab 8 Company Profile (read-only legal fields), and secure password rotation.

---

## 1. Architectural Highlights & Workflow

Module 9 triển khai toàn diện phân hệ quản lý định danh và gia nhập đối tác B2B:

### 1.1. Luồng Đăng Ký 3 Giai Đoạn ([`register/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/register/page.tsx) & [`onboarding/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/onboarding/page.tsx))
- Khác với e-commerce B2C thông thường, nền tảng B2B yêu cầu thẩm định tư cách pháp nhân (MST, Giấy phép ĐKKD, người đại diện).
- Ngay sau khi bấm nộp hồ sơ, API `POST /api/v1/auth/register-b2b` **cấp ngay tài khoản** ở trạng thái `PENDING_ADMIN` và trả về token session để đối tác đăng nhập theo dõi tiến độ trên thanh [`B2bOnboardingStepper`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/stepper.tsx):
  - **Stage 1 (`PENDING_ADMIN`)**: Hiển thị bảng tóm tắt hồ sơ và thông báo đang chờ Admin duyệt trong 24h.
  - **Stage 1 Bị Từ Chối (`REJECTED`)**: Hiển thị cảnh báo đỏ kèm lý do từ chối cụ thể (`rejection_reason`). Người dùng có thể bấm nút **"Chỉnh sửa & Nộp lại hồ sơ"** mở modal cập nhật thông tin và gọi `PUT /api/v1/b2b/onboarding/reapply` mà không cần tạo tài khoản mới.
  - **Stage 2 Duyệt Sơ Bộ (`ADMIN_APPROVED`)**: Hiển thị ghi chú của Admin (`approval_notice`). Đối tác kiểm tra lại thông tin và bấm **"Xác nhận tham gia & Kích hoạt B2B Portal"** (`POST /api/v1/b2b/onboarding/confirm`), backend nâng trạng thái lên `ACTIVE` và cấp token chính thức.
  - **Stage 3 Kích Hoạt (`ACTIVE`)**: Mở khóa toàn bộ quyền hạn và dẫn vào B2B Partner Dashboard (`/portal`).

### 1.2. Đăng Nhập Thông Minh Theo Vai Trò ([`login/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/login/page.tsx))
- Form đăng nhập hỗ trợ tài khoản test nhanh cho quản trị viên và đối tác mẫu.
- Phân luồng điều hướng tự động:
  - `ADMIN`, `MANAGER`, `SELLER` $\rightarrow$ `/admin`.
  - `B2B_PARTNER` đang `ACTIVE` $\rightarrow$ `/portal`.
  - `B2B_PARTNER` đang `PENDING_ADMIN`, `ADMIN_APPROVED`, `REJECTED` $\rightarrow$ `/onboarding`.

### 1.3. Hồ Sơ Doanh Nghiệp & Đổi Mật Khẩu ([`profile/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/profile/page.tsx))
- **Tab Hồ sơ**:
  - Khóa chỉ đọc các thông tin pháp lý đã thẩm định: Tên công ty, Mã số thuế, Loại hình kinh doanh, Người đại diện, Link GPKD.
  - Cho phép đối tác tự cập nhật: Người phụ trách thu mua, Số điện thoại liên hệ, Địa chỉ giao hàng mặc định (`PUT /api/v1/b2b/profile`).
- **Tab Bảo mật**:
  - Đổi mật khẩu an toàn (`POST /api/v1/auth/change-password`). Nhờ cơ chế Token Versioning ở Backend (Module 2), tất cả các phiên đăng nhập khác của tài khoản lập tức bị thu hồi.

---

## 2. Kết Quả Biên Dịch

- `npm run build`:
  - `Route /login`: 2.01 kB, First Load JS: 120 kB (bọc `<Suspense>` chuẩn Next.js 15).
  - `Route /register`: 3.17 kB, First Load JS: 121 kB.
  - `Route /onboarding`: 4.24 kB, First Load JS: 119 kB.
  - `Route /profile`: 3.34 kB, First Load JS: 118 kB.
  - **Compiled successfully in 1220ms (10/10 static pages), 0 TypeScript errors, 0 lint warnings**.
