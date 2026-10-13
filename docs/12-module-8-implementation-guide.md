# Module 8 Implementation Guide — Public Store Locator (PostGIS & Google Maps) & B2B Policy Page

> **Target Audience**: Mid-to-Senior Fullstack Engineers & UI/UX Designers  
> **Module Scope**: Dual-column Store Locator matching WordPress `ktech-map` plugin, Browser GPS & Geocoding address search, Spatial radius filtering (1km–100km & Toàn quốc), Interactive SVG radar & branch visualizer, Google Maps navigation deep linking, and Dynamic B2B Policy CMS rendering.

---

## 1. Architectural Overview & UI Engineering

Trong Module 8, chúng ta hoàn thiện phân hệ tra cứu công cộng (Public Storefront):

### 1.1. Store Locator Hai Cột (Chuẩn Hóa Từ Plugin `ktech-map` Sang Next.js 15)
Hệ thống mẫu WordPress dùng Daum Postcode và Kakao Map (thị trường Hàn Quốc). Tại Việt Nam, hệ thống được tái cấu trúc thành:
- **Cột Trái — Danh mục & Thanh tìm kiếm ([`stores/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/stores/page.tsx))**:
  - Ô nhập địa chỉ chuẩn với nút **"Vị trí GPS của tôi"** (`navigator.geolocation.getCurrentPosition`).
  - Thanh chọn bán kính đa nấc: `1km`, `3km`, `5km`, `10km`, `15km`, `20km`, `50km`, `100km`, `Toàn quốc`.
  - Danh sách chi nhánh hiển thị theo thẻ ([`BranchCard`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/store-locator/branch-card.tsx)):
    - Ưu tiên hiển thị các chi nhánh có cờ ghim `isPinned = true` (⭐ Ghim ưu tiên).
    - Tự động sắp xếp theo khoảng cách tăng dần từ vị trí tìm kiếm.
    - Hiển thị badge phân loại (`categoryTag`: Kho tổng, Đại lý...), giờ hoạt động, số hotline và nút chuyển tiếp sang ứng dụng Google Maps Navigation.
- **Cột Phải — Bản Đồ Tương Tác ([`store-map.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/store-locator/store-map.tsx))**:
  - Bộ hiển thị tọa độ không gian chính xác cao:
    - Tâm định vị người dùng (User GPS Pin) kèm hiệu ứng sóng radar tỏa ra.
    - Vòng tròn bán kính tìm kiếm thực tế (Radius Circle).
    - Các điểm ghim chi nhánh (Marker ghim màu vàng hổ phách, marker thường màu xanh/xám, marker đang chọn có vòng halo phát sáng).
    - Hỗ trợ công cụ phóng to (`ZoomIn`), thu nhỏ (`ZoomOut`), đặt lại góc nhìn (`ResetView`).
    - Khi người dùng click vào thẻ chi nhánh ở cột trái, bản đồ tự động pan & highlight điểm đó kèm thanh thông tin tóm tắt bên dưới.

### 1.2. Trang Chính Sách Đối Tác B2B ([`policy/page.tsx`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/app/policy/page.tsx))
- Kết nối trực tiếp với API CMS của Backend Module 5: `GET /api/v1/public/policy`.
- Hiển thị nội dung rich-text và 4 thẻ nguyên tắc cốt lõi:
  1. Bảng giá theo bậc lũy tiến do Admin cấu hình tự do.
  2. Chính sách nhận mẫu thử 0đ (Free Sample) cho đối tác ACTIVE.
  3. Đơn vị vận chuyển linh hoạt (GHN, GHTK, giao hỏa tốc AhaMove).
  4. Cam kết đổi trả hàng trong 48 giờ.

---

## 2. Kết Quả Biên Dịch

- `npm run build`:
  - `Route /stores`: 8.95 kB, First Load JS: 119 kB.
  - `Route /policy`: 5.1 kB, First Load JS: 118 kB.
  - **Compiled successfully in 1355ms (6/6 static pages), 0 TypeScript errors, 0 lint warnings**.
