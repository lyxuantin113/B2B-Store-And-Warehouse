-- ==============================================================================
-- V2__seed_rbac_and_default_configs.sql
-- Seeds initial RBAC Roles, Granular Permissions, and Default System Configs
-- ==============================================================================

-- 1. Seed Roles
INSERT INTO roles (id, code, name, description) VALUES
    (1, 'ADMIN',       'System Administrator', 'Full platform access including RBAC, 3rd-party API keys, and system policies'),
    (2, 'MANAGER',     'Operations Manager',   'Manages B2B partner approvals, multi-branch inventory, catalog, and orders'),
    (3, 'SELLER',      'Sales & Fulfillment',  'Processes assigned orders, updates waybill tracking numbers, and chats with partners'),
    (4, 'B2B_PARTNER', 'B2B Wholesale Partner','Verified business customer ordering wholesale tiers and free product samples')
ON CONFLICT (code) DO NOTHING;

-- 2. Seed Granular Permissions
INSERT INTO permissions (id, code, module, description) VALUES
    (1,  'partner:read',       'CRM',          'View B2B partner applications and company profiles'),
    (2,  'partner:approve',    'CRM',          'Approve, reject, or suspend B2B partner accounts'),
    (3,  'branch:write',       'WAREHOUSE',    'Create and update store/warehouse branches'),
    (4,  'inventory:read',     'WAREHOUSE',    'View branch stock levels and reservations'),
    (5,  'inventory:write',    'WAREHOUSE',    'Adjust branch stock levels and safety thresholds'),
    (6,  'catalog:write',      'CATALOG',      'Create and update categories, products, and wholesale price tiers'),
    (7,  'order:read',         'ORDER',        'View wholesale and sample orders'),
    (8,  'order:write',        'ORDER',        'Place wholesale or sample orders (B2B Partner)'),
    (9,  'order:manage',       'ORDER',        'Update order status, import/export waybills, and add admin memos'),
    (10, 'announcement:write', 'NOTIFICATION', 'Publish, edit, or unpublish broadcast announcements'),
    (11, 'policy:write',       'CMS',          'Update B2B wholesale & sample policy page content'),
    (12, 'config:manage',      'SYSTEM',       'Configure VNPay, MoMo, VietQR, GHN, GHTK, AhaMove, and Google Maps API keys'),
    (13, 'rbac:manage',        'SECURITY',     'Manage admin accounts, roles, and permissions')
ON CONFLICT (code) DO NOTHING;

-- 3. Map Permissions to ADMIN (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
ON CONFLICT DO NOTHING;

-- 4. Map Permissions to MANAGER (all operational permissions except config:manage & rbac:manage)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code NOT IN ('config:manage', 'rbac:manage')
ON CONFLICT DO NOTHING;

-- 5. Map Permissions to SELLER (partner:read, inventory:read, order:read, order:manage)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE code IN ('partner:read', 'inventory:read', 'order:read', 'order:manage')
ON CONFLICT DO NOTHING;

-- 6. Map Permissions to B2B_PARTNER (order:read, order:write)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 4, id FROM permissions WHERE code IN ('order:read', 'order:write')
ON CONFLICT DO NOTHING;

-- 7. Seed Default System Configurations (Policy, Payment, Shipping, Map)
INSERT INTO system_configs (config_key, config_group, config_value, is_encrypted) VALUES
    ('B2B_POLICY_HTML', 'POLICY', '<h3>B2B Wholesale &amp; Sample Policy</h3><ul><li><strong>Tier 1 (1~3 Boxes):</strong> 15% off retail price</li><li><strong>Tier 2 (4~6 Boxes):</strong> 25% off retail price</li><li><strong>Tier 3 (7+ Boxes):</strong> 35% off retail price</li><li><strong>Free Sample Policy:</strong> Product cost is 0 VND; partner only pays carrier shipping fee.</li></ul>', FALSE),
    ('PAYMENT_ENABLED_PROVIDERS', 'PAYMENT', '["VNPAY","MOMO","VIETQR","BANK_TRANSFER"]', FALSE),
    ('SHIPPING_ENABLED_PROVIDERS', 'SHIPPING', '["GHN","GHTK","AHAMOVE"]', FALSE),
    ('MAP_DEFAULT_RADIUS_KM', 'MAP', '10', FALSE)
ON CONFLICT (config_key) DO NOTHING;
