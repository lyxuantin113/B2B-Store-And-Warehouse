# B2B STORE & WAREHOUSE PLATFORM — MODULE 10 IMPLEMENTATION GUIDE

## 1. Overview & Architecture: Full B2B Partner Portal (`/portal`)

Module 10 establishes the complete **Enterprise B2B Partner Portal** with an 8-tab operational workbench. The portal connects verified business partners directly to multi-branch warehouse inventories, dynamic wholesale pricing tiers, atomic stock reservations, payment gateways (VNPay, MoMo, VietQR), shipping logistics (GHN, GHTK, AhaMove), free sample requests, and real-time notifications.

```
                                  +---------------------------------------+
                                  |   B2B Partner Portal (/portal)        |
                                  |   Guarded: accountStatus === 'ACTIVE' |
                                  +---------------------------------------+
                                                      |
    +-------------+-------------+-------------+-------+-------+-------------+-------------+-------------+
    |             |             |             |               |             |             |             |
Tab 1         Tab 2         Tab 3         Tab 4           Tab 5         Tab 6         Tab 7         Tab 8
Overview      Wholesale     Catalog       Orders          Notifs        Policy        Free Samples  Profile
Dashboard     Matrix Order  Wholesale     History         Center        B2B           Engine        Summary
4 KPIs        Auto-tiers    Tier Tables   Keyset Page     Broadcast     Discount      0 VND Goods   Tax Code
Recent List   One-Page Co   Stock levels  Cancel Pending  Atomic Read   Rules         Shipping Fee  License
```

---

## 2. The 8 Tabs Detailed Specifications

### Tab 1: Overview Dashboard (`overview-tab.tsx`)
- **Backend API**: `GET /api/v1/b2b/dashboard/overview`
- **KPI Metrics**:
  - `totalOrders`: Tổng số lượng đơn hàng đã đặt
  - `totalSpent`: Tổng doanh số mua hàng (VND)
  - `pendingOrders`: Đơn hàng đang chờ xử lý & xuất kho
  - `totalSampleRequests`: Tổng số lượt đăng ký hàng mẫu thử
- **Features**:
  - Welcome banner with partner company name and legal status.
  - Quick action shortcuts to Tabs 2, 4, and 7.
  - Table of 4 most recent orders with status badges and quick modal snapshots.

### Tab 2: Wholesale Matrix & One-Page Checkout (`order-matrix-tab.tsx`)
- **Backend APIs**:
  - `GET /api/v1/public/catalog/products`
  - `GET /api/v1/public/branches`
  - `POST /api/v1/b2b/orders/wholesale`
- **Features**:
  - **Dynamic Tier Highlighting**: Automatically calculates and highlights the active wholesale price tier (`tierLabel`, `unitPrice`) based on cart quantity.
  - **Warehouse Branch Selection**: Choose origin warehouse for inventory dispatch.
  - **One-Page Checkout Form**:
    - Pre-filled recipient data from legal profile (`contactPersonName`, `contactPhone`, `shippingAddressLine`).
    - Shipping provider selector (`GHN`, `AHAMOVE`, `GHTK`, `SELF_PICKUP`).
    - Payment method selector (`VNPAY`, `MOMO`, `VIETQR`, `BANK_TRANSFER`).
  - **Order Confirmation & Payment Modal**:
    - Generates dynamic VietQR Napas247 payment code with embedded order code and amount.
    - Redirect buttons for VNPay and MoMo payment gateways.

### Tab 3: B2B Wholesale Catalog (`catalog-tab.tsx`)
- **Features**:
  - Category pill filter bar + live SKU/Product name search.
  - Product cards displaying retail base price vs wholesale tier breakdown table (`minQty` ~ `maxQty` -> `unitPrice`).
  - Packaging specification badges and stock availability indicators.

### Tab 4: Order History & Keyset Pagination (`orders-tab.tsx`)
- **Backend APIs**:
  - `GET /api/v1/b2b/orders` with keyset params (`cursorCreatedAt`, `cursorId`, `orderType`, `orderStatus`, `limit`).
  - `POST /api/v1/b2b/orders/{orderCode}/cancel`
- **Features**:
  - Dual filters: Order Type (`WHOLESALE`, `SAMPLE`) and Order Status (`PENDING`, `CONFIRMED`, `SHIPPING`, `DELIVERED`, `CANCELLED`).
  - Order snapshot modal with itemized line totals, applied tier codes, and payment status.
  - Atomic order cancellation: Cancels `PENDING` orders and immediately releases reserved warehouse stock.

### Tab 5: Unified Notification Center (`notifications-tab.tsx`)
- **Backend APIs**:
  - `GET /api/v1/b2b/notifications?unreadOnly={bool}`
  - `PATCH /api/v1/b2b/notifications/{id}/read`
  - `PATCH /api/v1/b2b/notifications/read-all`
- **Features**:
  - Dual feeds: Admin broadcast announcements (pinned first with category badges and rich HTML preview modal) + personal order status notifications.
  - Unread badge counter with filter toggle (`Tất cả` vs `Chưa đọc`).
  - Single-click atomic mark-all-read.

### Tab 6: B2B Policy (`policy`)
- **Features**:
  - Embedded wholesale discount guidelines (Tier 1 ~ Tier 3).
  - Free sample rules and warehouse logistics standards.
  - Deep link to full legal documentation at `/policy`.

### Tab 7: Free Sample Request Engine (`sample-request-tab.tsx`)
- **Backend API**: `POST /api/v1/b2b/orders/sample`
- **Business Rules**:
  - Product selection limited to items where `sampleAllowed === true` or `sampleEnabled === true`.
  - Strict sample quota enforcement (1–3 units/product max).
  - **Product Subtotal = 0 VNĐ** (`appliedTierCode: "FREE_SAMPLE"`).
  - Partner only pays real-time estimated shipping fee via VNPay, MoMo, or VietQR.

### Tab 8: Partner Legal Profile Summary (`profile`)
- **Features**:
  - Quick legal summary (Tax Code, Business License verification status, Representative Name, Default Shipping Address).
  - Direct navigation button to `/profile` for legal amendments and password changes.

---

## 3. Verification & Build Health

### Frontend Route Verification
```bash
Route (app)                                 Size  First Load JS
┌ ○ /                                      162 B         106 kB
├ ○ /_not-found                            994 B         104 kB
├ ○ /login                               2.01 kB         120 kB
├ ○ /onboarding                          4.69 kB         119 kB
├ ○ /policy                               5.2 kB         119 kB
├ ○ /portal                              18.9 kB         136 kB
├ ○ /profile                             3.82 kB         118 kB
├ ○ /register                            3.17 kB         121 kB
└ ○ /stores                              9.11 kB         119 kB
+ First Load JS shared by all             103 kB
```
- **Next.js 15 Build**: 11/11 routes compiled, typed, and prerendered statically with zero errors.

### Backend JUnit 5 Tests
- **All 56 unit and integration tests passing**:
  - `OrderServiceTest` (4/4)
  - `StoreLocatorAndSystemConfigTest` (4/4)
  - `WebSocketAuthChannelInterceptorTest` (4/4)
  - `ChatServiceTest` (5/5)
  - `DynamicPriceTierValidatorTest` (4/4)
  - `InventoryServiceTest` (2/2)
  - `NotificationOutboxAndCronJobTest` (3/3)
  - `B2bPartnerServiceTest` (5/5)
  - `GatewayRouterAndSignatureTest` (3/3)
  - All Crypto, Token, and Security test suites passing.
