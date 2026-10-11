# 04. Cấu Trúc Dự Án Chuẩn (Spring Boot + Next.js), DevOps (Docker / K8s) & Chiến Lược Unit Test

Tài liệu này quy định cấu trúc thư mục chuẩn Enterprise cho **Backend (Java 21 + Spring Boot 3.4)** và **Frontend (Next.js 15 App Router + TypeScript + TailwindCSS)**, kiến trúc container hóa với **Docker Compose / Kubernetes (K8s)** và tiêu chuẩn kiểm thử tự động với **JUnit 5 + Mockito**.

---

## 1. Cấu Trúc Thư Mục Toàn Dự Án (Monorepo Hoặc Multi-Module Chuẩn)

```text
Master_Java/
├── PROJECT_CONTEXT.md                  # Tài liệu Context tổng thể của dự án
├── docs/                               # Tài liệu kiến trúc chuyên sâu (DB, API, Async, DevOps)
├── docker-compose.yml                  # Môi trường Local/Staging: Postgres+PostGIS, Redis, Kafka, MinIO, BE, FE
├── k8s/                                # Manifests triển khai Kubernetes (Deployment, Service, Ingress, HPA, ConfigMap)
├── backend/                            # Java 21 + Spring Boot 3.4 (Modular Clean Architecture)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/umego/b2b/
│       │   │   ├── common/             # ApiResponse, PageResponse, GlobalExceptionHandler, Security/XSS Utils, Constants
│       │   │   ├── config/             # SecurityConfig, JwtConfig, RedisConfig, KafkaConfig, OpenApiSwaggerConfig, WebSocketConfig
│       │   │   ├── security/           # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetails, RbacPermissionEvaluator
│       │   │   ├── modules/
│       │   │   │   ├── auth/           # Login, Dual-Token Refresh, Logout, Token Family Rotation
│       │   │   │   ├── user/           # Quản lý Users, Roles, Permissions (RBAC), Phân công chi nhánh
│       │   │   │   ├── partner/        # B2B 3-Stage Onboarding (Register, Re-apply, Admin Approve, Partner Confirm)
│       │   │   │   ├── branch/         # Multi-location Branches, Store Locator (PostGIS), Import/Export CSV
│       │   │   │   ├── catalog/        # Categories, Products, Tiered Wholesale Pricing (product_tier_prices)
│       │   │   │   ├── inventory/      # Branch Inventories, Atomic Stock Reservation/Deduction, Audit Logs
│       │   │   │   ├── order/          # Wholesale Orders, Free Sample Orders, Excel Waybill Import/Export
│       │   │   │   ├── payment/        # Strategy + Adapters: VNPay, MoMo, VietQR (PayOS/SePay)
│       │   │   │   ├── shipping/       # Strategy + Adapters: GHN, GHTK, AhaMove Express
│       │   │   │   ├── notification/   # Unified Notifications + Admin Broadcast Announcements
│       │   │   │   ├── analytics/      # 6 KPIs, Combo Charts (7d/1m/1y), Top 5 Products, Top 5 B2B Partners
│       │   │   │   ├── system/         # System Configs (Payment/Shipping/Map Keys), B2B Policy CMS
│       │   │   │   └── chat/           # (Phase 2) WebSocket STOMP Controllers & Chat Room Service
│       │   │   └── jobs/               # Cron Jobs (@Scheduled + ShedLock): OrderTimeout, TrackingSync, AnalyticsRollup
│       │   └── resources/
│       │       ├── db/migration/       # Flyway SQL Migrations (V1__init_schema.sql, V2__seed_rbac.sql...)
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── application-prod.yml
│       └── test/
│           └── java/com/umego/b2b/     # JUnit 5 + Mockito Unit Tests & Testcontainers Integration Tests
│
└── frontend/                           # Next.js 15 (App Router) + TypeScript + TailwindCSS
    ├── Dockerfile
    ├── package.json
    ├── tailwind.config.ts
    └── src/
        ├── app/
        │   ├── (public)/               # Giao diện giới thiệu doanh nghiệp & sản phẩm bên ngoài
        │   │   ├── page.tsx            # Trang chủ (Brand Showcase)
        │   │   ├── about/page.tsx      # Giới thiệu doanh nghiệp
        │   │   ├── products/           # Danh sách & Chi tiết sản phẩm trưng bày
        │   │   ├── stores/page.tsx     # Tìm cửa hàng gần nhất theo địa chỉ & bán kính (Google Maps)
        │   │   ├── b2b-register/       # Trang Đăng ký B2B & Theo dõi Stepper 3 giai đoạn
        │   │   └── login/page.tsx      # Đăng nhập B2B / Admin
        │   ├── (b2b-portal)/           # B2B Partner Dashboard riêng (Protected Route: yêu cầu ACTIVE)
        │   │   └── portal/
        │   │       ├── layout.tsx      # Sidebar 8 Tabs + Header thông báo + Quick Contact
        │   │       ├── page.tsx        # Tab 1: Dashboard Overview (4 KPIs, Biểu đồ 6 tháng, Donut)
        │   │       ├── quick-order/    # Tab 2: Ma trận đặt sỉ nhanh theo bậc giá + One-Page Checkout
        │   │       ├── catalog/        # Tab 3: Toàn bộ sản phẩm & Bảng giá sỉ 3 bậc
        │   │       ├── orders/         # Tab 4: Lịch sử đơn hàng (Sỉ & Mẫu thử) + Modal chi tiết
        │   │       ├── notifications/  # Tab 5: Thông báo đơn hàng & Thông báo chung
        │   │       ├── policy/         # Tab 6: Chính sách hợp tác B2B
        │   │       ├── sample-request/ # Tab 7: Đăng ký nhận mẫu thử (chỉ trả phí ship)
        │   │       └── profile/        # Tab 8: Hồ sơ doanh nghiệp & Đổi mật khẩu
        │   └── (admin)/                # Admin / Manager / Seller Portal (Protected theo RBAC)
        │       └── admin/
        │           ├── partners/       # Quản lý & Xét duyệt B2B Partner (4 KPIs + Bulk Actions)
        │           ├── branches/       # Quản lý Chi nhánh (Map Picker kéo thả + CSV Import/Export)
        │           ├── products/       # Quản lý Danh mục, Sản phẩm & Bảng giá bậc thang
        │           ├── inventories/    # Quản lý Tồn kho đa chi nhánh
        │           ├── orders/         # Quản lý Đơn hàng (6 KPIs + Đẩy đơn GHN/AhaMove + Excel)
        │           ├── analytics/      # Báo cáo phân tích doanh thu, Top 5 SP, Top 5 KH
        │           ├── announcements/  # Quản lý Thông báo chung
        │           ├── policy/         # Soạn thảo trang Chính sách B2B
        │           ├── users-rbac/     # Quản lý tài khoản Admin/Manager/Seller & Phân quyền RBAC
        │           └── settings/       # Cấu hình API VNPay, MoMo, VietQR, GHN, AhaMove, Google Maps
        ├── components/                 # UI Components tái sử dụng (Stepper, TierPriceTable, GoogleMapLocator, ChatWidget)
        ├── lib/                        # Axios/Fetch Interceptor (Auto Refresh Token Queue), Utils
        ├── hooks/                      # Custom React Hooks (useAuth, useCartTierPricing, useGoogleMaps)
        └── types/                      # TypeScript Interfaces đồng bộ với OpenAPI/Swagger DTOs
```

---

## 2. Thiết Kế Docker Compose & Kubernetes (DevOps Stack)

### 2.1. Kiến trúc `docker-compose.yml` chuẩn cho Local Development & Staging
Bao gồm 6 dịch vụ gọn nhẹ, khởi chạy chỉ với 1 lệnh `docker compose up -d`:
1. **`postgres`**: Image `postgis/postgis:16-3.4-alpine` (tích hợp sẵn PostgreSQL 16 + PostGIS cho tìm kiếm bán kính cửa hàng).
2. **`redis`**: Image `redis:7.2-alpine` (lưu trữ JWT Refresh Token Family, Access Token Blacklist, Cache KPI Dashboard, Bucket4j Rate Limit và ShedLock).
3. **`kafka`**: Image `apache/kafka:3.8.0` chạy ở chế độ **KRaft** (không cần container ZooKeeper, tiết kiệm 50% RAM).
4. **`minio`**: Image `minio/minio` giả lập AWS S3 tại local để lưu trữ bảo mật file Giấy phép ĐKKD và ảnh sản phẩm.
5. **`backend`**: Spring Boot 3.4 container (Multi-stage Dockerfile với `eclipse-temurin:21-jre-alpine`, cấu hình JVM container-aware `-XX:+UseZGC -XX:MaxRAMPercentage=75.0`).
6. **`frontend`**: Next.js 15 standalone build (`output: 'standalone'`), tối ưu kích thước image dưới 150MB.

### 2.2. Thiết kế Học tập & Mở rộng lên Kubernetes (K8s)
Thư mục `k8s/` cung cấp bộ manifest thực chiến để thực hành triển khai Cloud-Native:
- **`Deployment` + `LivenessProbe` / `ReadinessProbe`** kết nối với Spring Boot Actuator (`/actuator/health/liveness`, `/actuator/health/readiness`).
- **`HorizontalPodAutoscaler (HPA)`**: Tự động scale từ 2 lên 5 Pods khi CPU vượt ngưỡng 70%.
- **`ConfigMap` & `Secret`**: Tách biệt hoàn toàn cấu hình môi trường và thông tin nhạy cảm.

---

## 3. Chiến Lược Kiểm Thử Tự Động (JUnit 5 + Mockito + AssertJ)

Mọi nghiệp vụ cốt lõi phải có Unit Test với **JUnit 5 + Mockito** (độ phủ nhánh logic $\ge 80\%$ ở tầng Domain Service):

1. **`TieredPricingCalculatorTest`**:
   - Kiểm tra tính đúng đơn giá bậc 1 (`1~3 thùng`), bậc 2 (`4~6 thùng`), bậc 3 (`>= 7 thùng`).
   - Kiểm tra chống gian lận giá: khi client cố tình gửi `unitPrice = 1000` nhưng bảng `product_tier_prices` quy định `500.000`, hệ thống bắt buộc áp giá `500.000`.
2. **`B2bOnboardingServiceTest`**:
   - Kiểm tra luồng chuyển trạng thái hợp lệ: `PENDING_ADMIN` $\rightarrow$ `ADMIN_APPROVED` $\rightarrow$ `ACTIVE`.
   - Kiểm tra luồng từ chối & nộp lại: `PENDING_ADMIN` $\rightarrow$ `REJECTED` (bắt buộc có `rejectionReason`) $\rightarrow$ Partner gọi `reapply()` $\rightarrow$ quay về `PENDING_ADMIN` và tăng `reapplyCount`.
   - Đảm bảo ném `IllegalStateException` nếu Partner cố tình gọi `confirm()` khi đang ở `PENDING_ADMIN` hoặc `REJECTED`.
3. **`InventoryConcurrencyServiceTest`**:
   - Kiểm tra logic giữ chỗ tồn kho (`reserveStock`): khi tồn khả dụng (`quantity_on_hand - quantity_reserved`) nhỏ hơn số lượng đặt, ném `InsufficientStockException` và không phát sinh đơn hàng.
   - Kiểm tra hoàn trả tồn kho (`releaseReservedStock`) khi đơn hàng bị hủy hoặc quá hạn thanh toán 24h.
4. **`SampleOrderServiceTest`**:
   - Đảm bảo tài khoản chưa `ACTIVE` bị chặn đặt mẫu thử (`AccessDeniedException`).
   - Đảm bảo đơn mẫu thử (`orderType = SAMPLE`) luôn có `subtotalAmount = 0` và `totalAmount = shippingFee + paymentSurcharge`.
5. **`JwtTokenRotationServiceTest`**:
   - Kiểm tra phát hiện đánh cắp Refresh Token (Token Replay Attack): khi 1 `oldRefreshToken` đã được đổi sang token mới mà bị gửi lên lần thứ 2, toàn bộ `tokenFamilyId` trên Redis bị xóa sạch.
