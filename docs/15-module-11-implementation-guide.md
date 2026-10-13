# B2B STORE & WAREHOUSE PLATFORM — MODULE 11 IMPLEMENTATION GUIDE

## 1. Overview & Architecture: Admin & Seller Management Portal (`/admin`)

Module 11 establishes the complete **Enterprise Admin & Seller Management Portal** (`/admin`), providing a comprehensive backoffice suite for Administrators, Warehouse Managers, and Sales Representatives (`ADMIN`, `MANAGER`, `SELLER`).

```
                                  +---------------------------------------+
                                  |    Admin Backoffice Portal (/admin)   |
                                  |    Guarded: role in [ADMIN, MGR, SEL] |
                                  +---------------------------------------+
                                                      |
    +-------------+-------------+-------------+-------+-------+-------------+-------------+-------------+
    |             |             |             |               |             |             |             |
Page 1        Page 2        Page 3        Page 4          Page 5        Page 6        Page 7        Page 8
Dashboard     Partner CRM   Orders        Inventory       Branches      Catalog       Live Chat     Settings
6 Sales KPIs  4 KPIs Review Fulfillment   Multi-Branch    PostGIS Locs  Dynamic Tiers STOMP WS      AES-256-GCM
Revenue Trend Re-apply      Bulk Waybill  Safety Stock    Store Locator Quotas        Agent Desk    RBAC Staff
```

---

## 2. Pages & Modules Detailed Specifications

### 1. Executive Sales Dashboard (`/admin/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/orders/analytics/kpis`
  - `GET /api/v1/admin/partners/kpis`
  - `GET /api/v1/admin/inventory/low-stock-alerts`
- **6 Key Performance Indicators (KPIs)**:
  1. `totalRevenue`: Tổng doanh thu đơn sỉ tích lũy thành công (VND).
  2. `totalOrders`: Tổng số đơn hàng tiếp nhận (đơn sỉ & mẫu thử).
  3. `averageOrderValue` (AOV): Giá trị trung bình của mỗi đơn hàng sỉ.
  4. `paidOrdersCount`: Số đơn đã quyết toán tự động qua VNPay / MoMo / VietQR.
  5. `deliveryCompletionRatePercent`: Tỷ lệ giao hàng thành công qua GHN / AhaMove.
  6. `cancellationRatePercent`: Tỷ lệ hủy đơn (quá hạn thanh toán 24h & hủy kho).
- **Widgets**:
  - Tóm tắt tiến độ thẩm định đối tác B2B (Chờ thẩm định, Chờ xác nhận, Đã kích hoạt).
  - Bảng theo dõi cảnh báo cạn kho thời gian thực (Low Stock Alerts Watchlist).

### 2. B2B Partner CRM (`/admin/partners/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/partners/kpis`
  - `GET /api/v1/admin/partners?status=...&keyword=...`
  - `PATCH /api/v1/admin/partners/{id}/status`
  - `POST /api/v1/admin/partners/bulk-status`
- **Features**:
  - **3-Stage Review Workflow**:
    - **Stage 1 (`PENDING_ADMIN`)**: Kiểm tra giấy phép ĐKKD (xem file đính kèm PDF/JPG).
    - **Stage 2 (`ADMIN_APPROVED`)**: Duyệt sơ bộ kèm lời nhắn hướng dẫn (`approvalNotice`).
    - **Stage 3 (`ACTIVE`)**: Kích hoạt chính thức mở quyền đặt hàng sỉ.
    - **Từ chối (`REJECTED`)**: Nhập lý do từ chối chi tiết (`rejectionReason`) để doanh nghiệp nộp lại hồ sơ (`reapplyCount`).
    - **Tạm khóa (`SUSPENDED`)**: Ngưng giao dịch tạm thời.
  - **Thao Tác Hàng Loạt (Bulk Actions)**: Chọn nhiều đối tác để duyệt sơ bộ hoặc kích hoạt đồng loạt.

### 3. Xử Lý Đơn Hàng & Vận Đơn (`/admin/orders/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/orders` (Keyset pagination)
  - `PATCH /api/v1/admin/orders/{code}/status`
  - `POST /api/v1/admin/orders/bulk-waybill`
- **Features**:
  - Bộ lọc đa chiều: Phân loại (`WHOLESALE` / `SAMPLE`), Trạng thái đơn, Trạng thái thanh toán (`PAID` / `UNPAID`).
  - Quy trình xử lý xuất kho (Fulfillment):
    - `PENDING` $\rightarrow$ `CONFIRMED` (Duyệt xuất kho).
    - `CONFIRMED` $\rightarrow$ `SHIPPED` (Gán mã vận đơn GHN / AhaMove / GHTK).
    - `SHIPPED` $\rightarrow$ `DELIVERED` (Hoàn thành đơn hàng).
    - Hủy đơn: Tự động giải phóng tồn kho nguyên tử (`quantity_reserved`).
  - **Nhập Mã Vận Đơn Hàng Loạt (Bulk Waybill Import)**: Cho phép xuất kho và gán mã vận đơn cho nhiều đơn hàng cùng lúc.
  - Modal Snapshot chi tiết đơn hàng: Xem danh sách mặt hàng, bậc giá sỉ áp dụng, thông tin người nhận.

### 4. Tồn Kho Đa Chi Nhánh & Mức An Toàn (`/admin/inventory/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/branches`
  - `GET /api/v1/admin/inventory/branches/{branchId}`
  - `GET /api/v1/admin/inventory/low-stock-alerts`
  - `PUT /api/v1/admin/inventory`
- **Features**:
  - Chuyển đổi nhanh giữa xem theo chi nhánh và xem danh sách cảnh báo cạn kho (`availableQty <= safetyStock`).
  - Theo dõi 3 chỉ số tồn: Tồn khả dụng (`availableQty`), Đang giữ chỗ nguyên tử theo đơn (`reservedQty`), Ngưỡng an toàn (`safetyStock`).
  - Modal điều chỉnh tồn kho và cập nhật ngưỡng an toàn cho từng cặp (Chi nhánh, Sản phẩm).

### 5. Quản Lý Kho & Chi Nhánh (`/admin/branches/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/branches`
  - `POST /api/v1/admin/branches`
  - `PUT /api/v1/admin/branches/{branchId}`
- **Features**:
  - Quản lý danh mục kho xuất hàng và địa điểm Store Locator công khai.
  - Cấu hình tọa độ PostGIS (`latitude`, `longitude`), cờ ghim ưu tiên bản đồ (`isPinned`), cờ kích hoạt xuất hàng (`isActive`), khung giờ làm việc và số điện thoại liên hệ.

### 6. Danh Mục & Bậc Giá Sỉ Động (`/admin/catalog/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/public/catalog/categories`
  - `GET /api/v1/public/catalog/products`
  - `PUT /api/v1/admin/catalog/products/{id}/price-tiers`
  - `PUT /api/v1/admin/catalog/products/{id}`
- **Features**:
  - **Cấu hình Bậc Giá Sỉ Động (Dynamic Price Tiers)**:
    - Thêm/sửa các khoảng số lượng liên tục (`minQty` ~ `maxQty`), đơn giá sỉ và nhãn hiển thị (VD: "1–3 Thùng", "4–6 Thùng", "$\ge$ 7 Thùng").
  - **Cấu hình Chính Sách Mẫu Thử (Sample Policy)**:
    - Bật/tắt cho phép nhận mẫu thử (`sampleEnabled`) và hạn mức số lượng tối đa (`sampleMaxQty`) với đơn giá 0 VNĐ.

### 7. Bàn Làm Việc Live Support Chat (`/admin/chat/page.tsx`)
- **Backend APIs & Protocols**:
  - `GET /api/v1/admin/chat/rooms` (REST)
  - `GET /api/v1/b2b/chat/rooms/{roomId}/messages` (REST)
  - `PATCH /api/v1/admin/chat/rooms/{roomId}/assign` (REST)
  - `PATCH /api/v1/admin/chat/rooms/{roomId}/status` (REST)
  - **WebSocket STOMP over SockJS**:
    - Topic thông báo quản trị: `/topic/admin.chat.notifications`
    - Topic phòng chat: `/topic/chat.room.{roomId}`
    - Topic biên nhận đã đọc: `/topic/chat.room.{roomId}.read`
    - Destination gửi tin: `/app/chat.send`
- **Features**:
  - Cột trái: Danh sách phòng chat đối tác với badge đếm tin chưa đọc, bộ lọc Đang mở/Đã đóng, thông tin nhân sự phụ trách.
  - Cột phải: Cửa sổ hội thoại trực tuyến thời gian thực, nút tiếp nhận phụ trách (`assignAdmin`), nút đóng/mở phòng chat.

### 8. Cấu Hình Hệ Thống, Chính Sách & Phân Quyền (`/admin/settings/page.tsx`)
- **Backend APIs**:
  - `GET /api/v1/admin/system-configs` & `PUT /api/v1/admin/system-configs`
  - `GET /api/v1/public/policy` & `PUT /api/v1/admin/system-configs/policy`
  - `GET /api/v1/admin/rbac/roles` & `POST /api/v1/admin/rbac/staff-accounts`
- **Features**:
  - **Khóa API Bảo Mật**: Quản lý khóa cổng thanh toán & giao vận được mã hóa tự động bằng **AES-256-GCM**.
  - **Policy CMS**: Trình soạn thảo văn bản chính sách B2B (HTML sanitized qua OWASP HTML Sanitizer).
  - **RBAC Matrix & Cấp Tài Khoản**: Hiển thị chi tiết ma trận quyền hạn cho `ADMIN`, `MANAGER`, `SELLER` và form khởi tạo tài khoản nhân viên nội bộ.

---

## 3. Kết Quả Kiểm Thử & Sức Khỏe Build

### Frontend Next.js 15 App Router Build
```
Route (app)                                 Size  First Load JS
┌ ○ /                                      162 B         106 kB
├ ○ /_not-found                            994 B         104 kB
├ ○ /admin                               5.58 kB         119 kB
├ ○ /admin/branches                      4.78 kB         118 kB
├ ○ /admin/catalog                       5.13 kB         119 kB
├ ○ /admin/chat                          4.87 kB         140 kB
├ ○ /admin/inventory                     4.02 kB         118 kB
├ ○ /admin/orders                        6.58 kB         120 kB
├ ○ /admin/partners                       6.4 kB         120 kB
├ ○ /admin/settings                      5.65 kB         119 kB
├ ○ /login                               2.42 kB         120 kB
├ ○ /onboarding                          7.96 kB         118 kB
├ ○ /policy                              5.22 kB         119 kB
├ ○ /portal                              19.3 kB         136 kB
├ ○ /profile                             3.78 kB         118 kB
├ ○ /register                            7.03 kB         120 kB
└ ○ /stores                              9.14 kB         119 kB
+ First Load JS shared by all             103 kB
```
- **Kết quả**: 19/19 routes biên dịch thành công, zero type errors.

### Backend JUnit 5 Tests
- **Kết quả**: **56/56 unit and integration tests passing** (`BUILD SUCCESS`).
