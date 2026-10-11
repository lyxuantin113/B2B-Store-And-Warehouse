# B2B Wholesale & Sample Management Platform — Project Context & Architecture Blueprint

> **Vai trò thiết kế**: Senior Fullstack Java Architect (10+ năm kinh nghiệm xây dựng hệ thống High-Traffic, Database Optimization, Distributed Systems & DevOps).  
> **Dự án tham chiếu gốc (WordPress Reference)**: `UMEGO F&B` (`/Users/xuantin/Local Sites/umego/app` — Theme `k-tech` & Plugin `ktech-map`, `ktech-monitor`).  
> **Mục tiêu chuyển đổi**: Tái cấu trúc toàn bộ nghiệp vụ từ nền tảng WordPress/WooCommerce (thị trường Hàn Quốc) sang kiến trúc **Enterprise Modern Stack (Java Spring Boot + Next.js TypeScript)** tối ưu cho **thị trường Việt Nam** (VNPay, MoMo, VietQR, GHN/GHTK, AhaMove, Google Maps), quy mô nhỏ gọn nhưng đạt chuẩn **High-Performance, High-Security, Zero Race-Condition & Cloud-Native**.

---

## 1. Tổng Quan Nghiệp Vụ (Domain Context từ Dự Án Mẫu `umego`)

Hệ thống là nền tảng **B2B E-Commerce & Partner Portal** kết hợp **Brand Showcase (Giới thiệu doanh nghiệp & Sản phẩm mẫu)** với 2 phân hệ chính:

### 1.1. Phân hệ Khách hàng & Đối tác Doanh nghiệp (Public & B2B Partner Portal)

1. **Giao diện Giới thiệu Doanh nghiệp (Public Storefront)**:
   - Trang chủ (Home), Giới thiệu (About Us), Danh mục & Chi tiết sản phẩm (hiển thị quy cách đóng gói, thông tin sản phẩm nhưng ẩn giá sỉ chi tiết hoặc yêu cầu đăng nhập B2B để đặt sỉ), Tin tức/Blog, Liên hệ.
   - **Tìm kiếm Cửa hàng / Chi nhánh gần nhất (Store Locator)**:
     - Tìm kiếm địa chỉ chuẩn thông qua **Google Maps Places Autocomplete & Geocoding API** (thay thế Daum Postcode & Kakao Map của bản Hàn).
     - Lấy vị trí GPS hiện tại của người dùng (`navigator.geolocation`) hoặc tọa độ địa chỉ vừa nhập để tìm kiếm các chi nhánh/đại lý trong bán kính tuỳ chọn (`1km`, `3km`, `5km`, `10km`, `20km`, `50km`, `100km`, `Toàn quốc`).
     - Hiển thị danh sách chi nhánh bên trái (ưu tiên chi nhánh ghim `is_pinned = true`, sắp xếp theo khoảng cách tăng dần) kết hợp bản đồ tương tác **Google Maps JS SDK** bên phải (vẽ vòng tròn bán kính, Marker Clustering, InfoWindow chỉ đường).

2. **Luồng Đăng ký Tài khoản B2B 3 Giai đoạn (3-Stage Onboarding Stepper)**:
   - **Đặc thù nghiệp vụ**: Ngay khi doanh nghiệp điền form đăng ký (Tên công ty, Loại hình kinh doanh, Người phụ trách, SĐT, Email, Mật khẩu, Mã số thuế, Địa chỉ chuẩn Google Maps + Địa chỉ chi tiết, Upload file Giấy phép ĐKKD PDF/JPG/PNG $\le 10\text{MB}$), hệ thống **cấp ngay tài khoản** ở trạng thái `PENDING_ADMIN` để doanh nghiệp đăng nhập theo dõi tiến độ xét duyệt trên thanh **Stepper 3 bước**:
     - **Stage 1 — Nộp hồ sơ & Chờ thẩm định (`PENDING_ADMIN` / `REJECTED`)**:
       - Nếu `PENDING_ADMIN`: Hiển thị bảng tóm tắt hồ sơ đã nộp + thông báo đang chờ Admin xét duyệt trong 24h làm việc.
       - Nếu `REJECTED`: Hiển thị cảnh báo đỏ kèm **Lý do từ chối (`rejection_reason`)** do Admin nhập, cho phép bấm **"Chỉnh sửa & Nộp lại hồ sơ (Re-apply)"** ngay trên tài khoản hiện tại mà không cần tạo tài khoản mới.
     - **Stage 2 — Duyệt sơ bộ & Xác nhận thông tin (`ADMIN_APPROVED`)**:
       - Admin kiểm tra GPKD hợp lệ và chuyển sang `ADMIN_APPROVED` kèm lời nhắn hướng dẫn (`approval_notice`).
       - B2B Partner đăng nhập sẽ thấy Bước 2 sáng lên, kiểm tra lại toàn bộ thông tin pháp nhân lần cuối và bấm nút **"Xác nhận thông tin & Hoàn tất gia nhập"**.
     - **Stage 3 — Chính thức kích hoạt (`ACTIVE`)**:
       - Hệ thống chuyển trạng thái sang `ACTIVE`, bắn thông báo chào mừng và mở khóa toàn bộ tính năng trong **B2B Partner Dashboard**. (Ngoài ra có trạng thái `INACTIVE` khi Admin tạm ngưng hợp tác).

3. **B2B Partner Dashboard (8 Tabs chuyên biệt)**:
   - **Tab 1 — Tổng quan (Dashboard Overview)**:
     - Карd chào mừng doanh nghiệp + Badge chứng nhận đối tác B2B.
     - 4 thẻ KPI thời gian thực: Tổng chi tiêu tích lũy, Tổng số đơn hàng, Đơn đang giao/xử lý, Bậc chiết khấu sỉ tối đa (Tier 3).
     - Biểu đồ cột (Bar Chart) doanh số đặt hàng 6 tháng gần nhất & Biểu đồ tròn (Donut Chart) cơ cấu trạng thái đơn hàng + Danh sách 4 đơn hàng mới nhất.
   - **Tab 2 — Đặt hàng sỉ nhanh (Quick Order Matrix) & Giỏ hàng One-Page Checkout**:
     - Bảng ma trận sản phẩm cho phép tìm nhanh theo SKU/Tên, chọn chi nhánh xuất kho (hoặc hệ thống tự động chọn kho gần nhất còn hàng), tăng/giảm số lượng theo đơn vị đóng gói (`Thùng/Box`, `Lốc`, `Kiện`).
     - **Giá sỉ lũy tiến theo bậc số lượng (Tiered Wholesale Pricing)**:
       - Ví dụ: Bậc 1 (`1 ~ 3 thùng`), Bậc 2 (`4 ~ 6 thùng`), Bậc 3 (`>= 7 thùng`). Khi user thay đổi số lượng, cột đơn giá bậc tương ứng tự động highlight và tính lại tổng tiền.
     - **Cột phải — One-Page Checkout**: Chọn địa chỉ nhận hàng (mặc định từ hồ sơ hoặc tìm địa chỉ mới qua Google Maps), chọn phương thức vận chuyển (**Giao tiêu chuẩn GHN/GHTK** hoặc **Giao hỏa tốc trong ngày AhaMove/GrabExpress** tính phí theo khoảng cách GPS thực tế), chọn cổng thanh toán (**VNPay**, **MoMo**, **VietQR chuyển khoản tự động**).
   - **Tab 3 — Danh mục Sản phẩm & Bảng giá sỉ (Wholesale Catalog)**:
     - Tra cứu toàn bộ danh mục, quy cách đóng gói (`package_spec`), tồn kho khả dụng và bảng giá sỉ từng bậc.
   - **Tab 4 — Quản lý Đơn hàng (Order History & Tracking)**:
     - Lọc đơn theo trạng thái, tìm theo mã đơn (`#UM-yymmdd-xxx`), phân biệt badge **Đơn sỉ (`WHOLESALE`)** và **Đơn mẫu thử (`SAMPLE`)**, tra cứu mã vận đơn trực tiếp và xem Popup chi tiết đơn hàng.
   - **Tab 5 — Thông báo (Unified Notifications)**:
     - Hợp nhất 2 luồng thông báo: **(1) Thông báo trạng thái đơn hàng/tài khoản riêng của từng Partner** và **(2) Thông báo chung (Broadcast Announcements) do Admin tạo**. Hỗ trợ đánh dấu đã đọc, đồng bộ thu hồi tức thì nếu Admin ẩn/xóa thông báo chung.
   - **Tab 6 — Chính sách B2B (B2B Policy)**:
     - Trang nội dung tĩnh (Rich Text / Markdown) hiển thị điều khoản hợp tác, chính sách chiết khấu, đổi trả và vận chuyển do Admin cập nhật.
   - **Tab 7 — Đăng ký Nhận mẫu thử (Free Sample Request Flow)**:
     - Chỉ dành cho tài khoản B2B đã `ACTIVE`.
     - Chọn sản phẩm mẫu từ Popup tìm kiếm, số lượng giới hạn theo cấu hình, **đơn giá sản phẩm = 0 VNĐ**, khách hàng **chỉ thanh toán phí vận chuyển** (GHN/GHTK hoặc AhaMove) qua VNPay/MoMo/VietQR.
   - **Tab 8 — Hồ sơ Doanh nghiệp (Company Profile) & Đổi mật khẩu**:
     - Khóa chỉ đọc (Read-only) các thông tin pháp nhân đã duyệt: *Tên công ty, Mã số thuế, Loại hình kinh doanh, Email đăng nhập, File Giấy phép ĐKKD*.
     - Cho phép chỉnh sửa: *Người phụ trách, Số điện thoại, Địa chỉ giao hàng mặc định*.
     - Đổi mật khẩu an toàn (yêu cầu mật khẩu hiện tại, không trùng mật khẩu cũ, thu hồi các phiên đăng nhập khác).

---

### 1.2. Phân hệ Quản trị (Admin / Manager / Seller Portal)

1. **Phân quyền động RBAC (`ADMIN`, `MANAGER`, `SELLER`)**:
   - Thiết kế mô hình `Users` $\leftrightarrow$ `UserRoles` $\leftrightarrow$ `Roles` $\leftrightarrow$ `RolePermissions` $\leftrightarrow$ `Permissions` kết hợp phân quyền theo Chi nhánh (`user_branch_assignments` cho `MANAGER` và `SELLER`).
2. **Dashboard Quản lý Đối tác B2B (B2B Partner CRM)**:
   - 4 thẻ KPI (Tổng đối tác, Mới trong 7 ngày, Chờ thẩm định, Từ chối/Tạm khóa), bộ lọc theo trạng thái & thời gian, thao tác duyệt sơ bộ (`ADMIN_APPROVED`), duyệt chính thức (`ACTIVE`), từ chối kèm lý do (`REJECTED`), tạm khóa (`INACTIVE`) và thao tác hàng loạt (Bulk Actions).
3. **Quản lý Đa chi nhánh (Multi-Location Branches) & Sản phẩm / Tồn kho đa kho**:
   - Quản lý danh sách Chi nhánh/Kho/Cửa hàng (địa chỉ Google Maps, tọa độ `lat/lng`, giờ mở cửa, trạng thái ghim, Import/Export CSV).
   - Quản lý Danh mục (đa cấp), Sản phẩm, Quy cách đóng gói B2B, Bảng giá bậc thang (`product_tier_prices`) và **Tồn kho riêng biệt cho từng loại sản phẩm tại từng chi nhánh (`branch_inventories`)** kèm lịch sử biến động kho (`inventory_transactions`).
4. **Quản lý Đơn hàng & Báo cáo Phân tích (Sales & Analytics Dashboard)**:
   - 6 thẻ KPI (Doanh thu kỳ + % tăng trưởng, Tổng đơn, AOV, Đối tác mới, Tỷ lệ giao thành công, Tỷ lệ hủy), Biểu đồ kép Doanh thu & Đơn hàng (7 ngày / 1 tháng / 1 năm), Top 5 sản phẩm bán chạy, Top 5 doanh nghiệp chi tiêu cao nhất.
   - Đẩy đơn tự động sang GHN/GHTK/AhaMove hoặc xuất/nhập Excel mã vận đơn hàng loạt.
5. **Cấu hình Hệ thống & Nội dung Chính sách**:
   - Quản lý cấu hình cổng thanh toán (VNPay, MoMo, VietQR), đơn vị vận chuyển (GHN, GHTK, AhaMove, phí ship mặc định cho đơn sỉ/đơn mẫu), Google Maps API Key (mã hóa AES-256-GCM các secret key trong DB).
   - Quản lý trang Nội dung Chính sách B2B & Quản lý Thông báo chung (Broadcast Announcements).

---

## 2. Bảng Ánh Xạ Chuyển Đổi Third-Party APIs (Korea $\rightarrow$ Vietnam / Global)

| Nhóm dịch vụ | Dự án mẫu WordPress (`umego` - Hàn Quốc) | Giải pháp chuẩn hóa tại Việt Nam (Spring Boot + Next.js) |
| :--- | :--- | :--- |
| **Cổng thanh toán** | Toss Payments, KakaoPay, NaverPay, KB Kookmin Bank | **VNPay**, **MoMo Business**, **VietQR (SePay / Casso / PayOS)** hỗ trợ Webhook IPN tự động gạch nợ |
| **Giao hàng tiêu chuẩn** | Logen Express (iLOGEN EDI OpenAPI) | **Giao Hàng Nhanh (GHN)** / **Giao Hàng Tiết Kiệm (GHTK)** (Tính phí, tạo vận đơn, webhook hành trình) |
| **Giao hàng hỏa tốc** | Kakao T Quick (Kakao Mobility API) | **AhaMove API** / **GrabExpress** (Tính phí realtime theo khoảng cách GPS từ Chi nhánh $\rightarrow$ Khách) |
| **Bản đồ & Địa chỉ** | Daum Postcode + Kakao Local/Map + Naver Map | **Google Maps Platform** (Places Autocomplete, Geocoding API, Distance Matrix, Maps JS SDK) |
| **Thông báo ZNS/Chat** | Kakao AlimTalk | **Email (Spring Mail + Thymeleaf)** + **Web Push / WebSocket In-App** (+ tùy chọn Zalo ZNS) |

---

## 3. Kiến Trúc Tổng Thể Hệ Thống (System Architecture)

Dù quy mô ban đầu nhỏ nhưng hệ thống được thiết kế theo hướng **Modular Monolith (Clean / Hexagonal-inspired Layered Architecture)**, sẵn sàng tách Microservices khi tải tăng cao:

```mermaid
flowchart TB
    subgraph Client["Frontend Layer (Next.js 15 App Router + TypeScript + TailwindCSS)"]
        PUB["Public Storefront & Store Locator"]
        B2B["B2B Partner Dashboard (8 Tabs)"]
        ADM["Admin / Manager / Seller Portal"]
    end

    subgraph Edge["Gateway & Security Layer"]
        NGINX["Nginx Reverse Proxy / Ingress (SSL, Rate Limit, Gzip)"]
    end

    subgraph App["Backend Layer (Java 21 + Spring Boot 3.4)"]
        SEC["Spring Security 6 + JWT Dual-Token Filter + RBAC"]
        API["REST Controllers (OpenAPI 3 / Swagger)"]
        WS["WebSocket STOMP Broker (Phase 2 Chatbox)"]
        SVC["Domain Services (Order, Inventory, Partner, Pricing, StoreLocator)"]
        CRON["Spring @Scheduled / ShedLock Cron Jobs"]
        PROD["Kafka Event Producer"]
        CONS["Kafka Event Consumer (Async Notification, Analytics, Audit)"]
    end

    subgraph Data["Data & Messaging Layer (Docker / K8s)"]
        PG[("PostgreSQL 16 + PostGIS\n(Primary DB)")]
        REDIS[("Redis 7\n(JWT Blacklist/Refresh, Cache, Distributed Lock)")]
        KAFKA[["Apache Kafka (KRaft Mode)\nOrder & Notification Events"]]
        S3[("MinIO / AWS S3\n(Business Licenses & Product Media)"]]
    end

    subgraph Ext["External Third-Party APIs"]
        PAY["VNPay / MoMo / PayOS (VietQR)"]
        SHIP["GHN / GHTK / AhaMove"]
        GMAP["Google Maps Platform (Places, Geocoding, JS Map)"]
    end

    Client --> NGINX --> SEC --> API --> SVC
    Client <-->|WebSocket STOMP| WS
    SVC --> PG
    SVC --> REDIS
    SVC --> S3
    SVC --> PROD --> KAFKA --> CONS
    CRON --> SVC
    SVC --> PAY
    SVC --> SHIP
    SVC --> GMAP
```

---

## 4. Các Giải Pháp Tối Ưu Trọng Tâm (Senior Optimization Playbook)

### 4.1. Bảo mật & Xác thực (Security & JWT Dual-Token)
- **JWT Dual-Token Mechanism**:
  - `Access Token` (Stateless JWT, TTL ngắn: `15 phút`, chứa `userId`, `email`, `roles`, `b2bStatus`, `branchIds`) gửi qua Header `Authorization: Bearer <token>`.
  - `Refresh Token` (TTL: `7 ngày`, lưu trong `HttpOnly, Secure, SameSite=Strict` Cookie + đối chiếu hash kèm `tokenFamilyId` trên **Redis**).
  - **Refresh Token Rotation & Reuse Detection**: Mỗi lần refresh sẽ cấp cặp token mới và vô hiệu hóa Refresh Token cũ. Nếu phát hiện Refresh Token cũ bị dùng lại (dấu hiệu đánh cắp token), hệ thống lập tức xóa toàn bộ `tokenFamilyId` trong Redis, buộc đăng nhập lại.
  - **Access Token Blacklist**: Khi Logout hoặc Đổi mật khẩu/Khóa tài khoản, lưu `jti` của Access Token vào Redis với TTL bằng thời gian sống còn lại của token.
- **Chống XSS, CSRF, SQL Injection & File Upload Attack**:
  - **XSS**: Sử dụng **OWASP Java HTML Sanitizer** (hoặc `Jsoup.clean`) cho nội dung Rich Text (Chính sách B2B, Thông báo chung), Jackson custom deserializer strip/escape script tags trên các chuỗi đầu vào, kèm HTTP Header `Content-Security-Policy (CSP)` và `X-Content-Type-Options: nosniff`.
  - **File Upload (Giấy phép ĐKKD)**: Kiểm tra **Magic Bytes (Apache Tika)** thực tế của file (chỉ cho phép `application/pdf`, `image/jpeg`, `image/png`), đổi tên file thành `UUID`, lưu trữ trên Object Storage (MinIO/S3) và chỉ cấp **Pre-signed URL có thời hạn** cho Admin hoặc chính chủ xem.
  - **Rate Limiting**: Áp dụng **Bucket4j + Redis** giới hạn tần suất theo IP/User cho các API nhạy cảm (`/api/v1/auth/**`, `/api/v1/b2b/register`, `/api/v1/stores/nearby`).

### 4.2. Tối ưu Cơ sở dữ liệu (SQL Optimization)
- **Triệt tiêu lỗi N+1 Query**:
  - Bắt buộc cấu hình `FetchType.LAZY` trên toàn bộ `@ManyToOne`, `@OneToMany`.
  - Sử dụng `@EntityGraph`, `JOIN FETCH` hoặc **Spring Data JPA Projections / Blaze-Persistence / QueryDSL** để lấy đúng các cột cần thiết trong 1 câu truy vấn duy nhất.
  - Tắt `spring.jpa.open-in-view=false` để phát hiện sớm mọi lỗi lazy loading ngoài tầng Service/Transaction.
- **Chiến lược Phân trang (Pagination)**:
  - **Offset Pagination** tối ưu (tách `countQuery` tối giản không `LEFT JOIN` thừa) cho các bảng quản trị dưới 10.000 bản ghi cần nhảy trang.
  - **Keyset / Cursor-based Pagination (`WHERE (created_at, id) < (:lastCreatedAt, :lastId)`)** cho danh sách Đơn hàng lớn, Lịch sử thông báo, Lịch sử tồn kho và Tin nhắn Chatbox (độ phức tạp $O(1)$ bất kể trang thứ bao nhiêu).
- **Chiến lược Đánh Index (Single, Composite, Partial & Spatial Index)**:
  - **Single Index**: `users(email)`, `b2b_profiles(tax_id)`, `products(sku)`, `orders(order_code)`.
  - **Composite Index (Tuân thủ quy tắc Leftmost Prefix & Equality $\rightarrow$ Sort $\rightarrow$ Range)**:
    - `idx_orders_partner_status_date ON orders (b2b_partner_id, status, created_at DESC)`
    - `idx_branch_inv_branch_prod ON branch_inventories (branch_id, product_id)`
    - `idx_notifications_user_read_date ON user_notifications (user_id, is_read, created_at DESC)`
  - **Spatial Index (PostGIS `GIST`)**:
    - `idx_branches_location_gist ON branches USING GIST (location)` giúp truy vấn cửa hàng gần nhất theo bán kính `ST_DWithin` trong vài mili-giây thay vì quét toàn bảng.

### 4.3. Chống Race Condition & Đảm bảo Tính Nhất Quán (Concurrency Control)
1. **Đặt hàng & Trừ tồn kho đa chi nhánh (Inventory Deduction)**:
   - Kết hợp **Atomic Conditional SQL Update** + **Optimistic Locking (`@Version`)**:
     ```sql
     UPDATE branch_inventories
     SET quantity_reserved = quantity_reserved + :qty,
         version = version + 1,
         updated_at = NOW()
     WHERE branch_id = :branchId
       AND product_id = :productId
       AND (quantity_on_hand - quantity_reserved) >= :qty;
     ```
   - Nếu `updatedRows == 0` $\rightarrow$ Ném `InsufficientStockException` ngay lập tức, tuyệt đối không bao giờ xảy ra tình trạng **Overselling (bán âm kho)** khi nhiều B2B Partner đặt sỉ cùng lúc.
2. **Chống Double-Submit Đặt hàng & Thanh toán (Idempotency Key)**:
   - Client gửi header `Idempotency-Key: <UUID>` khi bấm thanh toán. Backend dùng Redis `SET key value NX EX 300` để khóa xử lý trùng lặp trong 5 phút.
3. **Xử lý Webhook Thanh toán (VNPay / MoMo / VietQR)**:
   - Sử dụng `SELECT ... FOR UPDATE` (Pessimistic Write Lock) trên bản ghi `orders` hoặc `payment_transactions` khi nhận IPN Webhook để tránh trường hợp cổng thanh toán bắn 2 request webhook đồng thời làm cộng/trừ kho 2 lần.

### 4.4. Ứng dụng Kafka, Cron Job & K8s
- **Apache Kafka (Event-Driven)**:
  - Khi đơn hàng được tạo hoặc đổi trạng thái, luồng chính chỉ ghi DB + bắn sự kiện vào các topic `b2b.order.events`, `b2b.partner.events`, `b2b.notification.broadcast` (áp dụng **Transactional Outbox Pattern** để không mất message).
  - Consumer xử lý bất đồng bộ: gửi Email/ZNS, tạo Notification cho hàng loạt B2B Partner khi Admin đăng thông báo chung, đồng bộ bảng tổng hợp doanh thu (`daily_sales_metrics`), đẩy đơn sang GHN/GHTK.
- **Cron Jobs (Spring `@Scheduled` + `ShedLock` chống chạy trùng trên nhiều Pod K8s)**:
  1. **Tự động hủy đơn quá hạn thanh toán** (mỗi 5 phút): Quét các đơn `PENDING_PAYMENT` quá 24h $\rightarrow$ chuyển sang `CANCELLED` và giải phóng `quantity_reserved` về lại kho.
  2. **Đồng bộ trạng thái vận đơn định kỳ** (mỗi 30 phút): Đối soát các vận đơn đang giao với GHN/GHTK/AhaMove phòng trường hợp rớt webhook.
  3. **Tổng hợp dữ liệu Báo cáo Doanh thu & KPI ban đêm** (00:15 hàng ngày): Roll-up số liệu vào bảng `daily_branch_sales_summary` để Dashboard Analytics load tức thì.
  4. **Cảnh báo tồn kho tối thiểu (Low Stock Alert)** (08:00 hàng ngày): Gửi thông báo cho `MANAGER` các chi nhánh có mặt hàng chạm ngưỡng cảnh báo.

---

## 5. Danh Mục Tài Liệu Chi Tiết Trong Workspace

Toàn bộ thiết kế kỹ thuật chi tiết được chia thành các tài liệu chuyên sâu tại thư mục [`docs/`](file:///Users/xuantin/PersonalProjects/Master_Java/docs):

1. [`docs/01-database-erd-and-indexing.md`](file:///Users/xuantin/PersonalProjects/Master_Java/docs/01-database-erd-and-indexing.md): Sơ đồ ERD hoàn chỉnh, PostgreSQL DDL (RBAC, B2B 3-Stage Onboarding, Multi-Branch Inventory, Tier Pricing, Sample Orders, Notifications, Chat), chiến lược Index (Single/Composite/Partial/GiST) và giải pháp chống N+1 / Race Condition.
2. [`docs/02-api-and-security-spec.md`](file:///Users/xuantin/PersonalProjects/Master_Java/docs/02-api-and-security-spec.md): Thiết kế RESTful API chuẩn Swagger/OpenAPI 3, ma trận phân quyền RBAC (`ADMIN`, `MANAGER`, `SELLER`, `B2B_PARTNER`), kiến trúc JWT Dual-Token + Redis, phòng chống XSS/CSRF/Rate-Limit.
3. [`docs/03-third-party-and-async-workflows.md`](file:///Users/xuantin/PersonalProjects/Master_Java/docs/03-third-party-and-async-workflows.md): Thiết kế Adapter tích hợp VNPay, MoMo, VietQR, GHN/GHTK, AhaMove, Google Maps (Store Locator & Geocoding), kiến trúc Kafka Event-Driven, Cron Jobs (ShedLock) và WebSocket STOMP Chatbox (Phase 2).
4. [`docs/04-project-structure-and-devops.md`](file:///Users/xuantin/PersonalProjects/Master_Java/docs/04-project-structure-and-devops.md): Cấu trúc thư mục chuẩn cho Backend (Spring Boot 3 + Java 21) & Frontend (Next.js + TypeScript + Tailwind), cấu hình `docker-compose.yml`, Kubernetes (K8s) manifests và chiến lược Unit/Integration Test với JUnit 5 + Mockito + Testcontainers.

---

## 6. Quy Tắc Quản Lý Source Code & Git Workflow

- **Remote Repository**: `https://github.com/lyxuantin113/B2B-Store-And-Warehouse.git` (Branch chính: `main`).
- **Quy tắc bắt buộc khi phát triển**: Mỗi khi hoàn thành xong 1 module (hoặc 1 hạng mục kiến trúc/tính năng độc lập), bắt buộc kiểm tra trạng thái, chạy test/build liên quan, thực hiện `git add .`, `git commit -m "<conventional-commit-message>"` và `git push origin main` ngay lập tức.

