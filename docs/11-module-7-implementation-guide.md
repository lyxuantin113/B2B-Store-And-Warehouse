# Module 7 Implementation Guide — Frontend Architecture Foundation & Core Infrastructure (Next.js 15 + TypeScript + Tailwind CSS)

> **Target Audience**: Mid-to-Senior Fullstack Engineers & System Architects  
> **Module Scope**: Next.js 15 App Router setup, React 19, TypeScript strict mode, Enterprise Design System (Tailwind CSS, Lucide React), JWT Dual-Token Auto-Refresh Queue with Concurrency Lock, Stomp over SockJS Real-time Chat client, Auth & Toast Contexts, and Multi-stage Production Dockerfile.

---

## 1. Architectural Highlights & Engineering Decisions

Trong Module 7, chúng ta thiết lập toàn bộ nền móng kiến trúc phía Frontend (Next.js 15) sẵn sàng kết nối đồng bộ với hệ thống Backend Spring Boot 3.4 (Modules 1–6):

### 1.1. HTTP API Client Với JWT Dual-Token Auto-Refresh Queue ([`api-client.ts`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/lib/api-client.ts))
Một trong những lỗi phổ biến nhất ở frontend là **Race Condition khi Token hết hạn**:
- Giả sử trang Dashboard tải đồng thời 4 API (`/kpi`, `/orders`, `/notifications`, `/profile`). Cả 4 API cùng nhận lỗi HTTP `401 Unauthorized`.
- Nếu không xử lý concurrency, frontend sẽ bắn 4 request `/auth/refresh` song song. Do Backend áp dụng **Refresh Token Rotation & Reuse Detection** (Module 2), request refresh thứ hai sẽ bị phát hiện là token đã bị dùng $\rightarrow$ hệ thống lập tức kích hoạt chuông báo động bảo mật và đá văng người dùng ra trang đăng nhập!
- **Giải pháp Senior**:
  ```ts
  let isRefreshing = false;
  let refreshSubscribers: ((token: string) => void)[] = [];
  ```
  - Khi request đầu tiên gặp `401`, biến cờ `isRefreshing = true` được bật lên.
  - Các request tiếp theo cùng thời điểm sẽ không gọi API refresh nữa mà được đẩy vào hàng đợi `refreshSubscribers`.
  - Request đầu tiên gọi `/api/v1/auth/refresh` lấy cặp Access Token & Refresh Token mới thành công.
  - Sau đó, hàm `onTokenRefreshed(newToken)` duyệt toàn bộ hàng đợi, gán token mới và tự động retry tất cả request ban đầu.

### 1.2. Client WebSocket STOMP over SockJS ([`websocket-client.ts`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/lib/websocket-client.ts))
- Bọc thư viện `@stomp/stompjs` và `sockjs-client` thành một Service Singleton hướng đối tượng `StompChatClient`.
- Hỗ trợ truyền header `Authorization: Bearer <accessToken>` khi `CONNECT`.
- Quản lý vòng đời subscription, tự động hủy kênh cũ khi chuyển phòng chat, và reconnect tự động sau 5 giây nếu mất mạng.

### 1.3. UI Design System Phục Vụ Nghiệp Vụ B2B
- [`B2bOnboardingStepper`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/stepper.tsx): Trực quan hóa tiến trình xét duyệt hồ sơ doanh nghiệp 3 giai đoạn:
  - **Giai đoạn 1**: `PENDING_ADMIN` (đang thẩm định) hoặc `REJECTED` (hiển thị lý do từ chối và nút nộp lại).
  - **Giai đoạn 2**: `ADMIN_APPROVED` (duyệt sơ bộ, hiển thị ghi chú Admin và nút xác nhận).
  - **Giai đoạn 3**: `ACTIVE` (đã chính thức kích hoạt).
- [`ChatWidget`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/chat/chat-widget.tsx): Hộp thoại chat hỗ trợ nổi (floating widget) góc dưới bên phải, kết nối thời gian thực qua WebSocket STOMP, hiển thị badge tin chưa đọc và tự động gửi tín hiệu "Đã xem" khi mở hộp thoại.
- Bộ linh kiện dùng chung: [`Button`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/button.tsx), [`Input`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/input.tsx), [`Modal`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/modal.tsx), [`Badge`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/badge.tsx), [`Card`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/card.tsx), [`Tabs`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/tabs.tsx), [`Skeleton`](file:///Users/xuantin/PersonalProjects/Master_Java/frontend/src/components/ui/skeleton.tsx).

---

## 2. Cấu Trúc Thư Mục Frontend Chuẩn Hóa

```
frontend/
├── Dockerfile                  # Multi-stage production build (Node 22 Alpine, standalone runner)
├── package.json                # Next.js 15, React 19, Tailwind CSS, Lucide React, STOMP
├── tsconfig.json               # Path alias @/* -> ./src/*
├── tailwind.config.ts          # Bảng màu Enterprise (brand blue, slate dark mode)
├── next.config.ts              # API proxy rewrite /api/v1 -> Backend
└── src/
    ├── types/                  # Strict TypeScript contracts khớp 100% với Spring Boot DTOs
    │   ├── api.ts              # ApiResponse, PageDto, CursorPageDto, ErrorCode
    │   ├── auth.ts             # AccountStatus, RoleCode, UserSummary, B2bPartnerProfile
    │   ├── catalog.ts          # Category, Product, ProductPriceTier
    │   ├── branch.ts           # Branch, NearbyBranch, StandardizedAddress
    │   ├── order.ts            # Order, OrderItem, OrderStatus, PaymentMethod, ShippingProvider
    │   ├── notification.ts     # NotificationItem, AnnouncementItem, UnifiedNotificationCenter
    │   └── chat.ts             # ChatRoom, ChatMessage, ChatMessageType, ChatReadReceiptEvent
    ├── lib/
    │   ├── api-client.ts       # Centralized Fetch Client + JWT Auto-Refresh Queue
    │   ├── utils.ts            # formatVnd, formatDate, formatDistance, cn
    │   └── websocket-client.ts # STOMP over SockJS Client Singleton
    ├── context/
    │   ├── auth-context.tsx    # AuthProvider, useAuth (login, logout, refreshProfile, role flags)
    │   └── toast-context.tsx   # ToastProvider, useToast (floating alerts success/error/info/warn)
    ├── components/
    │   ├── ui/                 # Button, Input, Modal, Badge, Card, Stepper, Tabs, Skeleton
    │   ├── layout/             # Navbar, Footer
    │   └── chat/               # ChatWidget (WebSocket real-time chatbox)
    └── app/
        ├── layout.tsx          # RootLayout bọc Auth, Toast, Navbar, Footer, ChatWidget
        ├── page.tsx            # Trang chủ Showcase B2B & CTA tìm kho, nộp hồ sơ
        └── globals.css         # Tailwind base directives & custom scrollbars
```

---

## 3. Kiểm Thử Biên Dịch (Next.js 15 Production Build)

- Lệnh kiểm tra: `npm run build`
- Kết quả: **Compiled successfully in 1627ms, 4/4 static pages generated, 0 type errors, 0 lint warnings**.
