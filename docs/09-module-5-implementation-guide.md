# Module 5 Implementation Guide — Store Locator (PostGIS + Google Maps), Unified Notifications, Dynamic System Configs, Kafka Outbox Relay & ShedLock Cron Jobs

> **Target Audience**: Mid-to-Senior Java Backend Engineers & System Architects  
> **Module Scope**: Store Locator with Google Maps Address Standardization & PostGIS Spatial Radius Search, Unified Notification Center & Broadcast Announcements, Dynamic Encrypted API Configurations & B2B Policy CMS, Transactional Outbox Relay to Apache Kafka, and ShedLock Distributed Cron Jobs.

---

## 1. Architectural Overview & Core Engineering Challenges

In Module 5, we complete the entire backend platform by addressing **four critical production engineering challenges**:

1. **Google Maps Address Standardization & PostGIS `O(log N)` Radius Search (`com.b2b.warehouse.branch`)**:
   - Replaces the legacy Korean `Daum Postcode` and `Naver Map` scripts (`stores.php` & `address-search.js`).
   - Users can either pass their live browser GPS `(latitude, longitude)` or type a raw Vietnamese address (e.g., `"123 Nguyễn Văn Linh, Quận 7"`) which [`GoogleMapsGeocodingService`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/branch/service/GoogleMapsGeocodingService.java) standardizes into a canonical address + WGS84 coordinates + postal code.
   - [`BranchRepository.findNearbyBranchesWithinRadiusMeters`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/branch/repository/BranchRepository.java#L27-L44) executes `ST_DWithin` against the partial GIST index `idx_branches_geom_gist` (`ON branches USING GIST (geom) WHERE is_active = TRUE`), and [`BranchService`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/branch/service/BranchService.java) computes exact `distanceKm` and `distanceMeters`, sorting pinned flagship branches first (`is_pinned DESC, distance_meters ASC`).

2. **Unified Notification Center & Broadcast Announcements (`com.b2b.warehouse.notification`)**:
   - **Broadcast Announcements (`announcements`)**: Created by Admin/Managers (`announcement:write`). Rich HTML is sanitized via OWASP [`HtmlSanitizerUtil`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/common/util/HtmlSanitizerUtil.java) before persistence to prevent Stored XSS. Queried via partial composite index `idx_announcements_active_pinned_pub`.
   - **Personal Order & Approval Notifications (`notifications`)**: Queried via `O(1)` Keyset Pagination (`(created_at, id) < (:cursorCreatedAt, :cursorId)`) and partial index `idx_notifications_user_unread_created` (`WHERE is_read = FALSE`) for instant unread badge counts.

3. **Dynamic Encrypted Third-Party API Configs & B2B Policy CMS (`com.b2b.warehouse.systemconfig`)**:
   - **AES-256-GCM Encryption at Rest + UI Secret Masking**: When the Admin configures sensitive keys (`VNPAY_HASH_SECRET`, `MOMO_SECRET_KEY`, `GHN_API_TOKEN`, `GOOGLE_MAPS_API_KEY`) with `isEncrypted = true`, [`SystemConfigService`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/systemconfig/service/SystemConfigService.java) encrypts the value with a random 96-bit IV + 128-bit authentication tag before saving to PostgreSQL, and masks the value (`************9876`) in Admin API responses so secrets never leak to the browser.
   - **Single-Page Policy CMS (`B2B_POLICY_HTML`)**: Admin updates the B2B Wholesale & Sample Policy (`PUT /api/v1/admin/system-configs/policy` with `policy:write`), sanitized by OWASP HTML Sanitizer, and exposed to Public & B2B Partners at `GET /api/v1/public/policy` and `GET /api/v1/b2b/policy`.

4. **Transactional Outbox Relay to Kafka & ShedLock Distributed Cron Jobs (`com.b2b.warehouse.async`)**:
   - **[`OutboxEventRelayScheduler`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/async/OutboxEventRelayScheduler.java)**: Polls `outbox_events` using partial index `idx_outbox_pending_created` (`WHERE status = 'PENDING' ORDER BY created_at ASC`), publishes to Apache Kafka topic `b2b.domain.events` keyed by `aggregateId` (guaranteeing partition-level ordering per Order), and materializes notifications via [`DomainEventNotificationProcessor`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/async/DomainEventNotificationProcessor.java) / [`KafkaDomainEventConsumer`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/async/KafkaDomainEventConsumer.java). Includes Dead-Letter protection (`FAILED` after 5 retries).
   - **[`WarehouseMaintenanceCronScheduler`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/main/java/com/b2b/warehouse/async/WarehouseMaintenanceCronScheduler.java)**:
     - **Job 1 (`cancelExpiredUnpaidOrdersAndReleaseStock`)**: Runs every 15 minutes with `@SchedulerLock`. Finds `PENDING` + `UNPAID` orders older than 24 hours, atomically releases reserved stock back to `available_qty`, transitions order to `CANCELLED`, and notifies the B2B Partner.
     - **Job 2 (`scanLowStockAndEmitSafetyAlerts`)**: Runs every 6 hours with `@SchedulerLock`. Scans active branches where `available_qty <= safety_stock` and emits a `LOW_STOCK_SAFETY_ALERT` outbox event.

---

## 2. Architecture Diagram: Outbox Relay, Kafka & ShedLock Cron Jobs

```mermaid
flowchart LR
    subgraph DB["PostgreSQL 16 + PostGIS"]
        ORD[("orders & branch_inventory")]
        OUT[("outbox_events\n(idx_outbox_pending_created)")]
        NOTIF[("notifications &\nannouncements")]
        LOCK[("shedlock table")]
    end

    subgraph Async["Spring Boot Async & Scheduled Workers (com.b2b.warehouse.async)"]
        RELAY["OutboxEventRelayScheduler\n(@Scheduled + @SchedulerLock)"]
        CRON1["Stale Unpaid Order Cleanup\n(Every 15m + @SchedulerLock)"]
        CRON2["Low-Stock Safety Scanner\n(Every 6h + @SchedulerLock)"]
        PROC["DomainEventNotificationProcessor"]
    end

    subgraph Broker["Apache Kafka (KRaft)"]
        TOPIC[["Topic: b2b.domain.events\n(Key = aggregateId)"]]
    end

    LOCK -.->|Distributed Lock| RELAY
    LOCK -.->|Distributed Lock| CRON1
    LOCK -.->|Distributed Lock| CRON2

    OUT -->|Poll PENDING batch| RELAY
    RELAY -->|Publish Event| TOPIC
    TOPIC -->|@KafkaListener| PROC
    PROC -->|Insert Alert| NOTIF

    CRON1 -->|Release Reserved Stock & Cancel| ORD
    CRON1 -->|Notify Partner| NOTIF
    CRON2 -->|Detect available_qty <= safety_stock| ORD
    CRON2 -->|Emit LOW_STOCK_SAFETY_ALERT| OUT
```

---

## 3. Verification & Unit Testing Suite (JUnit 5 + Mockito)

Two new test suites were added in Module 5:
1. [`StoreLocatorAndSystemConfigTest.java`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/test/java/com/b2b/warehouse/branch/service/StoreLocatorAndSystemConfigTest.java):
   - Verifies Google Maps Vietnamese address standardization and coordinate resolution.
   - Verifies nearest store radius search (`10km`), filtering out far branches (Hanoi ~1,140 km away) and sorting pinned branches first.
   - Verifies `AES-256-GCM` encryption at rest and secret masking (`************9876`) in Admin responses.
   - Verifies OWASP XSS sanitization on `B2B_POLICY_HTML`.
2. [`NotificationOutboxAndCronJobTest.java`](file:///Users/xuantin/PersonalProjects/Master_Java/backend/src/test/java/com/b2b/warehouse/async/NotificationOutboxAndCronJobTest.java):
   - Verifies Broadcast Announcement XSS sanitization and Unified Notification Center aggregation.
   - Verifies `OutboxEventRelayScheduler` transitioning `PENDING` -> `PUBLISHED` and creating B2B Partner notifications.
   - Verifies `WarehouseMaintenanceCronScheduler` auto-cancelling 24h expired unpaid orders and atomically releasing reserved stock.
