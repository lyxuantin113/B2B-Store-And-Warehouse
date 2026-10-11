-- ==============================================================================
-- V1__init_schema_and_indexes.sql
-- B2B Wholesale & Free Sample Management Platform
-- PostgreSQL 16 + PostGIS 3.4 Schema & High-Traffic Indexing Strategy (ESR Rule)
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ==============================================================================
-- 1. RBAC & IDENTITY TABLES
-- ==============================================================================
CREATE TABLE roles (
    id          SMALLSERIAL PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE, -- ADMIN, MANAGER, SELLER, B2B_PARTNER
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id          SMALLSERIAL PRIMARY KEY,
    code        VARCHAR(80) NOT NULL UNIQUE,
    module      VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id       SMALLINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id SMALLINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(25) NOT NULL,
    role_id         SMALLINT NOT NULL REFERENCES roles(id),
    account_status  VARCHAR(30) NOT NULL DEFAULT 'PENDING_ADMIN'
        CHECK (account_status IN ('PENDING_ADMIN', 'ADMIN_APPROVED', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    token_version   INT NOT NULL DEFAULT 1,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE b2b_partner_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    company_name            VARCHAR(200) NOT NULL,
    tax_code                VARCHAR(30) NOT NULL UNIQUE,
    business_type           VARCHAR(80),
    representative_name     VARCHAR(120) NOT NULL,
    contact_person_name     VARCHAR(120) NOT NULL,
    contact_phone           VARCHAR(25) NOT NULL,
    business_license_url    VARCHAR(500) NOT NULL,
    postal_code             VARCHAR(20),
    address_line            VARCHAR(255) NOT NULL,
    address_detail          VARCHAR(255),
    shipping_postal_code    VARCHAR(20),
    shipping_address_line   VARCHAR(255),
    shipping_address_detail VARCHAR(255),
    approval_notice         TEXT,
    rejection_reason        TEXT,
    partner_Confirmed_at    TIMESTAMPTZ,
    approved_by             UUID REFERENCES users(id),
    approved_at             TIMESTAMPTZ,
    reapply_count           INT NOT NULL DEFAULT 0,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 2. MULTI-LOCATION BRANCHES & STORE LOCATOR (POSTGIS)
-- ==============================================================================
CREATE TABLE branches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    category_tag    VARCHAR(80),
    phone           VARCHAR(25),
    postal_code     VARCHAR(20),
    address_line    VARCHAR(255) NOT NULL,
    address_detail  VARCHAR(255),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    geom            GEOGRAPHY(Point, 4326) NOT NULL,
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    operating_hours VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 3. CATALOG, TIERED PRICING & MULTI-BRANCH INVENTORY
-- ==============================================================================
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES categories(id),
    slug        VARCHAR(120) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id         BIGINT NOT NULL REFERENCES categories(id),
    sku                 VARCHAR(60) NOT NULL UNIQUE,
    slug                VARCHAR(180) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    subtitle            VARCHAR(255),
    description         TEXT,
    unit_label          VARCHAR(40) NOT NULL DEFAULT 'Box (20ea)',
    units_per_box       INT NOT NULL DEFAULT 20,
    retail_price        NUMERIC(15, 2) NOT NULL CHECK (retail_price >= 0),
    base_wholesale_price NUMERIC(15, 2) NOT NULL CHECK (base_wholesale_price >= 0),
    sample_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    sample_max_qty      INT NOT NULL DEFAULT 2,
    weight_grams        INT NOT NULL DEFAULT 500,
    thumbnail_url       VARCHAR(500),
    gallery_urls        JSONB DEFAULT '[]'::jsonb,
    specs_json          JSONB DEFAULT '{}'::jsonb,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE product_price_tiers (
    id          BIGSERIAL PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tier_code   VARCHAR(20) NOT NULL, -- TIER_1_3, TIER_4_6, TIER_7_PLUS
    min_qty     INT NOT NULL CHECK (min_qty >= 1),
    max_qty     INT CHECK (max_qty IS NULL OR max_qty >= min_qty),
    unit_price  NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    UNIQUE (product_id, min_qty)
);

CREATE TABLE branch_inventory (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    available_qty   INT NOT NULL DEFAULT 0 CHECK (available_qty >= 0),
    reserved_qty    INT NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    safety_stock    INT NOT NULL DEFAULT 10 CHECK (safety_stock >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (branch_id, product_id)
);

-- ==============================================================================
-- 4. ORDERS, ORDER ITEMS & PAYMENT TRANSACTIONS
-- ==============================================================================
CREATE TABLE orders (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code              VARCHAR(40) NOT NULL UNIQUE,
    user_id                 UUID NOT NULL REFERENCES users(id),
    branch_id               UUID REFERENCES branches(id),
    order_type              VARCHAR(20) NOT NULL CHECK (order_type IN ('WHOLESALE', 'SAMPLE')),
    order_status            VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED')),
    payment_method          VARCHAR(30) NOT NULL
        CHECK (payment_method IN ('VNPAY', 'MOMO', 'VIETQR', 'BANK_TRANSFER')),
    payment_status          VARCHAR(30) NOT NULL DEFAULT 'UNPAID'
        CHECK (payment_status IN ('UNPAID', 'PENDING_VERIFY', 'PAID', 'FAILED', 'REFUNDED')),
    shipping_provider       VARCHAR(30) NOT NULL
        CHECK (shipping_provider IN ('GHN', 'GHTK', 'AHAMOVE', 'SELF_PICKUP')),
    tracking_number         VARCHAR(80),
    subtotal_amount         NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(15, 2) NOT NULL DEFAULT 0,
    shipping_fee            NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tax_amount              NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(15, 2) NOT NULL DEFAULT 0,
    recipient_name          VARCHAR(120) NOT NULL,
    recipient_phone         VARCHAR(25) NOT NULL,
    shipping_postal_code    VARCHAR(20),
    shipping_address_line   VARCHAR(255) NOT NULL,
    shipping_address_detail VARCHAR(255),
    order_note              TEXT,
    admin_memo              TEXT,
    version                 BIGINT NOT NULL DEFAULT 0,
    paid_at                 TIMESTAMPTZ,
    shipped_at              TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id          UUID NOT NULL REFERENCES products(id),
    sku_snapshot        VARCHAR(60) NOT NULL,
    product_name_snap   VARCHAR(200) NOT NULL,
    unit_label_snap     VARCHAR(40) NOT NULL,
    applied_tier_code   VARCHAR(20),
    quantity            INT NOT NULL CHECK (quantity > 0),
    unit_price          NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    line_total          NUMERIC(15, 2) NOT NULL CHECK (line_total >= 0)
);

CREATE TABLE payment_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    provider            VARCHAR(30) NOT NULL,
    idempotency_key     VARCHAR(100) NOT NULL UNIQUE,
    external_txn_id     VARCHAR(120),
    amount              NUMERIC(15, 2) NOT NULL,
    status              VARCHAR(30) NOT NULL CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED')),
    raw_webhook_payload JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 5. NOTIFICATIONS, ANNOUNCEMENTS, SYSTEM CONFIGS & OUTBOX
-- ==============================================================================
CREATE TABLE announcements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    content_html    TEXT NOT NULL,
    category_badge  VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    is_published    BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID REFERENCES users(id),
    published_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notif_type      VARCHAR(40) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    reference_id    UUID,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE system_configs (
    config_key      VARCHAR(100) PRIMARY KEY,
    config_group    VARCHAR(40) NOT NULL, -- PAYMENT, SHIPPING, MAP, POLICY
    config_value    TEXT NOT NULL,
    is_encrypted    BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(80) NOT NULL,
    event_type      VARCHAR(80) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

-- ==============================================================================
-- 6. REAL-TIME B2B SUPPORT CHAT (WEBSOCKET PHASE 2) & SHEDLOCK
-- ==============================================================================
CREATE TABLE chat_rooms (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_user_id     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    assigned_admin_id   UUID REFERENCES users(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    last_message_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    room_id         UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    message_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'ORDER_REF')),
    content         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- ==============================================================================
-- 7. HIGH-TRAFFIC INDEXING STRATEGY (SINGLE, COMPOSITE ESR, PARTIAL, GIST)
-- ==============================================================================

-- 7.1 PostGIS Spatial Index for Store Locator (ST_DWithin & <-> KNN distance sort)
CREATE INDEX idx_branches_geom_gist
    ON branches USING GIST (geom)
    WHERE is_active = TRUE;

-- 7.2 B2B Partner CRM Admin Filter + Keyset Pagination
CREATE INDEX idx_users_role_status_created
    ON users (role_id, account_status, created_at DESC)
    WHERE deleted_at IS NULL;

-- 7.3 Partner Order History & Keyset Pagination (ESR: user_id, order_type -> created_at DESC)
CREATE INDEX idx_orders_user_type_created
    ON orders (user_id, order_type, created_at DESC, id DESC);

-- 7.4 Admin Order Management & Analytics (ESR: order_status, payment_status -> created_at DESC)
CREATE INDEX idx_orders_status_payment_created
    ON orders (order_status, payment_status, created_at DESC, id DESC);

-- 7.5 Order Items Foreign Key Batch Lookup
CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);

-- 7.6 Product Catalog Active Lookup by Category + Sort
CREATE INDEX idx_products_category_active_created
    ON products (category_id, created_at DESC)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- 7.7 Unread Notifications Badge & Feed (Partial Composite Index)
CREATE INDEX idx_notifications_user_unread_created
    ON notifications (user_id, created_at DESC)
    WHERE is_read = FALSE;

-- 7.8 Active Broadcast Announcements (Auto-hides unpublished or soft-deleted notices)
CREATE INDEX idx_announcements_active_pinned_pub
    ON announcements (is_pinned DESC, published_at DESC)
    WHERE is_published = TRUE AND deleted_at IS NULL;

-- 7.9 Transactional Outbox Polling Index (Partial Index on PENDING events only)
CREATE INDEX idx_outbox_pending_created
    ON outbox_events (created_at ASC)
    WHERE status = 'PENDING';
