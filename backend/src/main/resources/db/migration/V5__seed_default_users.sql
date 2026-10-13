-- ==============================================================================
-- V5__seed_default_users.sql
-- Seeds Default Users (Admin, Manager, Seller, B2B Partners), Partner Profiles,
-- Multi-branch Stores, Wholesale Catalog, Tiered Pricing, Inventory & Sample Orders
-- ==============================================================================

-- 1. SEED DEFAULT USERS (Password for all accounts is: Admin@123456)
-- BCrypt cost 12 hashed via pgcrypto crypt(..., gen_salt('bf', 12))

-- 1.1 Admin Warehouse (FE Quick Demo 1)
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@warehouse.com',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Tổng Quản Trị Hệ Thống',
    '0901234567',
    1,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.2 Admin UMEGO
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'admin@umego.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Quản Trị Viên UMEGO',
    '0901234568',
    1,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.3 Operations Manager
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'manager@umego.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Trần Quản Lý Kho Vận',
    '0902345678',
    2,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.4 Sales Staff
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000004',
    'seller@umego.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Nguyễn Nhân Viên Bán Hàng',
    '0903456789',
    3,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.5 Active B2B Partner - VinMart (FE Quick Demo 2)
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000005',
    'partner@vinmart.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Nguyễn Văn Thu Mua WinMart',
    '0904567890',
    4,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.6 Active B2B Partner - Dược Phẩm ABC
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000006',
    'partner@congtyabc.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Lê Hoàng Đối Tác ABC',
    '0905678901',
    4,
    'ACTIVE',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'ACTIVE';

-- 1.7 Pending B2B Partner - Đang Chờ Duyệt (For testing Review / Approval workflow)
INSERT INTO users (id, email, password_hash, full_name, phone, role_id, account_status, token_version)
VALUES (
    'a0000000-0000-0000-0000-000000000007',
    'pending@doanhnghiep.vn',
    crypt('Admin@123456', gen_salt('bf', 12)),
    'Võ Minh Chờ Duyệt',
    '0906789012',
    4,
    'PENDING_ADMIN',
    1
) ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 12)),
    account_status = 'PENDING_ADMIN';

-- 2. SEED B2B PARTNER PROFILES
INSERT INTO b2b_partner_profiles (
    id, user_id, company_name, tax_code, business_type, representative_name,
    contact_person_name, contact_phone, business_license_url, postal_code,
    address_line, address_detail, shipping_postal_code, shipping_address_line,
    shipping_address_detail, approval_notice, partner_confirmed_at, approved_at, reapply_count
) VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000005',
    'Công ty Cổ phần Dịch vụ Thương mại Tổng hợp WinCommerce',
    '0104918404',
    'Chuỗi Bán Lẻ & Bán Sỉ Siêu Thị',
    'Trương Công Thắng',
    'Nguyễn Văn Thu Mua WinMart',
    '0904567890',
    'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800',
    '700000',
    'Tầng 5, Tòa nhà MPlaza Saigon, 39 Lê Duẩn, Bến Nghé, Quận 1, TP.HCM',
    'Phòng Thu Mua & Cung Ứng',
    '700000',
    'Kho Tổng WinCommerce Sóng Thần, TP. Dĩ An, Tỉnh Bình Dương',
    'Cổng Tiếp Nhận Hàng Sỉ B2B',
    'Hồ sơ doanh nghiệp hợp lệ. Đã duyệt hạn mức sỉ cấp 1.',
    NOW(),
    NOW(),
    0
) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO b2b_partner_profiles (
    id, user_id, company_name, tax_code, business_type, representative_name,
    contact_person_name, contact_phone, business_license_url, postal_code,
    address_line, address_detail, shipping_postal_code, shipping_address_line,
    shipping_address_detail, approval_notice, partner_confirmed_at, approved_at, reapply_count
) VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000006',
    'Công ty TNHH Phân Phối Dược Mỹ Phẩm ABC',
    '0312345678',
    'Phân Phối Dược & Mỹ Phẩm',
    'Lê Hoàng Đối Tác ABC',
    'Lê Hoàng Đối Tác ABC',
    '0905678901',
    'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800',
    '700000',
    '123 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP.HCM',
    'Tầng 3 Tòa Nhà Bitexco Nam Long',
    '700000',
    '123 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP.HCM',
    'Kho hàng tầng trệt',
    'Hồ sơ được chấp thuận ngày 01/10/2026',
    NOW(),
    NOW(),
    0
) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO b2b_partner_profiles (
    id, user_id, company_name, tax_code, business_type, representative_name,
    contact_person_name, contact_phone, business_license_url, postal_code,
    address_line, address_detail, shipping_postal_code, shipping_address_line,
    shipping_address_detail, reapply_count
) VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000007',
    'Công ty Cổ phần Đầu tư Thương mại Tân Thời Đại',
    '0319876543',
    'Đại Lý Bán Buôn Tiêu Dùng',
    'Võ Minh Chờ Duyệt',
    'Võ Minh Chờ Duyệt',
    '0906789012',
    'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800',
    '700000',
    '456 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP.HCM',
    'Lầu 4 Văn phòng Đại diện',
    '700000',
    '456 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP.HCM',
    'Kho trung tâm Tân Thời Đại',
    0
) ON CONFLICT (user_id) DO NOTHING;

-- 3. SEED MULTI-LOCATION BRANCHES (PostGIS Geometry)
INSERT INTO branches (id, code, name, category_tag, phone, postal_code, address_line, address_detail, latitude, longitude, geom, is_pinned, is_active, operating_hours)
VALUES
    (
        'c0000000-0000-0000-0000-000000000001',
        'WH-HCM-01',
        'Kho Tổng Tân Bình - TP. Hồ Chí Minh',
        'KHO_TONG',
        '02838112233',
        '700000',
        '102 Bạch Đằng, Phường 2, Tân Bình, TP. Hồ Chí Minh',
        'Cổng số 2 - Cụm Kho Sân Bay Tân Sơn Nhất',
        10.8015,
        106.6575,
        ST_SetSRID(ST_MakePoint(106.6575, 10.8015), 4326),
        TRUE,
        TRUE,
        '07:30 - 21:00 (Thứ 2 - Thứ 7)'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'WH-HN-01',
        'Kho Trung Tâm Cầu Giấy - Hà Nội',
        'KHO_TRUNG_TAM',
        '02437889900',
        '100000',
        '88 Duy Tân, Phường Dịch Vọng Hậu, Cầu Giấy, Hà Nội',
        'Khu Tiểu Thủ Công Nghiệp Cầu Giấy',
        21.0333,
        105.7833,
        ST_SetSRID(ST_MakePoint(105.7833, 21.0333), 4326),
        TRUE,
        TRUE,
        '08:00 - 20:30 (Thứ 2 - Thứ 7)'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'WH-DN-01',
        'Kho Miền Trung Hải Châu - Đà Nẵng',
        'CHI_NHANH',
        '02363556677',
        '550000',
        '15 Nguyễn Tri Phương, Phường Chính Gián, Thanh Khê, Đà Nẵng',
        'Gần ngã tư Điện Biên Phủ',
        16.0544,
        108.2022,
        ST_SetSRID(ST_MakePoint(108.2022, 16.0544), 4326),
        FALSE,
        TRUE,
        '08:00 - 18:00 (Thứ 2 - Thứ 6)'
    )
ON CONFLICT (code) DO NOTHING;

-- 4. SEED CATEGORIES
INSERT INTO categories (id, slug, name, sort_order, is_active)
VALUES
    (1, 'nuoc-uong-dong-chai', 'Nước Uống & Đồ Uống Đóng Chai', 1, TRUE),
    (2, 'thuc-pham-kho-dong-goi', 'Thực Phẩm Khô & Nông Sản Chế Biến', 2, TRUE),
    (3, 'hoa-my-pham-ve-sinh', 'Hóa Mỹ Phẩm & Chăm Sóc Sức Khỏe', 3, TRUE),
    (4, 'banh-keo-an-vat', 'Bánh Kẹo & Đồ Ăn Vặt Cao Cấp', 4, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. SEED PRODUCTS
INSERT INTO products (
    id, category_id, sku, slug, name, subtitle, description,
    unit_label, units_per_box, retail_price, base_wholesale_price,
    sample_enabled, sample_max_qty, weight_grams, thumbnail_url,
    gallery_urls, specs_json, is_active
) VALUES
    (
        'd0000000-0000-0000-0000-000000000001',
        1,
        'CF-ESP-01',
        'ca-phe-hoa-tan-espresso-dark-roast',
        'Cà Phê Hòa Tan Espresso Dark Roast UMEGO',
        'Hương vị đậm đà nguyên bản từ hạt Arabica Cầu Đất',
        'Sản phẩm cà phê hòa tan cao cấp chiết xuất bằng công nghệ sấy lạnh hiện đại, giữ trọn hương thơm nồng nàn và hậu vị ngọt sâu. Đóng hộp 20 gói tiện lợi cho văn phòng và chuỗi nhà hàng.',
        'Hộp (20 gói)',
        20,
        120000.00,
        85000.00,
        TRUE,
        2,
        320,
        'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=600',
        '["https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=800", "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800"]'::jsonb,
        '{"origin": "Lâm Đồng, Việt Nam", "shelf_life": "24 tháng", "packaging": "Hộp giấy cán màng kim loại"}'::jsonb,
        TRUE
    ),
    (
        'd0000000-0000-0000-0000-000000000002',
        1,
        'TEA-OOL-02',
        'tra-xanh-oolong-thuong-hang-tay-bac',
        'Trà Xanh Oolong Thượng Hạng Mộc Châu',
        'Hái tay búp 1 tôm 2 lá, sao hương truyền thống',
        'Trà Ô long Mộc Châu trồng ở độ cao trên 1000m quanh năm sương phủ. Nước trà vàng óng ánh mật ong, vị chát thanh dịu và dư vị ngọt ngào lưu luyến.',
        'Lon (100g)',
        24,
        160000.00,
        110000.00,
        TRUE,
        2,
        250,
        'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=600',
        '["https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=800"]'::jsonb,
        '{"origin": "Mộc Châu, Sơn La", "shelf_life": "18 tháng", "packaging": "Lon thiếc cao cấp hút chân không"}'::jsonb,
        TRUE
    ),
    (
        'd0000000-0000-0000-0000-000000000003',
        2,
        'FS-PQ-03',
        'nuoc-mam-cot-ca-com-phu-quoc-40-do-dam',
        'Nước Mắm Cốt Cá Cơm Phú Quốc 40 Độ Đạm',
        'Ủ chượp thùng gỗ bời lời 18 tháng tự nhiên',
        'Nước mắm truyền thống Phú Quốc được làm từ 100% cá cơm than tươi và muối hạt Bà Rịa. Không chất điều vị, không hương liệu tổng hợp, đạt tiêu chuẩn xuất khẩu FDA và HACCP.',
        'Thùng (12 chai 500ml)',
        12,
        480000.00,
        360000.00,
        TRUE,
        1,
        7200,
        'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=600',
        '["https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800"]'::jsonb,
        '{"origin": "Phú Quốc, Kiên Giang", "protein": "40g N/l", "shelf_life": "36 tháng"}'::jsonb,
        TRUE
    ),
    (
        'd0000000-0000-0000-0000-000000000004',
        2,
        'MLK-MAC-04',
        'sua-hat-macca-hanh-nhan-nguyen-chat',
        'Sữa Hạt Macca Hạnh Nhân Nguyên Chất Không Đường',
        'Dinh dưỡng thuần chay từ hạt tự nhiên cao cấp',
        'Sản phẩm sữa thực vật tiệt trùng UHT không chứa lactose, không biến đổi gen (Non-GMO), thích hợp cho mọi lứa tuổi và đối tượng ăn kiêng thuần chay.',
        'Thùng (24 lốc 4 hộp)',
        24,
        680000.00,
        510000.00,
        TRUE,
        2,
        5400,
        'https://images.unsplash.com/photo-1550583724-b2692b85b150?w=600',
        '["https://images.unsplash.com/photo-1550583724-b2692b85b150?w=800"]'::jsonb,
        '{"origin": "Việt Nam", "shelf_life": "12 tháng", "packaging": "Thùng carton 24 lốc Tetra Pak"}'::jsonb,
        TRUE
    ),
    (
        'd0000000-0000-0000-0000-000000000005',
        3,
        'SHP-BK-05',
        'dau-goi-bo-ket-buoi-thao-moc',
        'Dầu Gội Thảo Mộc Bồ Kết & Tinh Dầu Vỏ Bưởi',
        'Giảm gãy rụng, kích thích mọc tóc tự nhiên không Sulfate',
        'Công thức cô đặc từ bồ kết nướng than hoa, cỏ mần trầu, hà thủ ô và tinh dầu vỏ bưởi da xanh ép lạnh. Phù hợp cho chuỗi spa và nhà phân phối mỹ phẩm thiên nhiên.',
        'Thùng (20 chai 500ml)',
        20,
        2400000.00,
        1800000.00,
        TRUE,
        1,
        11000,
        'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=600',
        '["https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=800"]'::jsonb,
        '{"volume": "500ml/chai", "standard": "CGMP ASEAN", "shelf_life": "24 tháng"}'::jsonb,
        TRUE
    )
ON CONFLICT (sku) DO NOTHING;

-- 6. SEED PRODUCT PRICE TIERS
-- Dynamic volume pricing: Tier 1 (1-3 boxes), Tier 2 (4-6 boxes), Tier 3 (7+ boxes)
INSERT INTO product_price_tiers (product_id, tier_code, min_qty, max_qty, unit_price, discount_percent)
VALUES
    -- Cà Phê Espresso
    ('d0000000-0000-0000-0000-000000000001', 'TIER_1_3', 1, 3, 85000.00, 29.17),
    ('d0000000-0000-0000-0000-000000000001', 'TIER_4_6', 4, 6, 75000.00, 37.50),
    ('d0000000-0000-0000-0000-000000000001', 'TIER_7_PLUS', 7, NULL, 65000.00, 45.83),
    -- Trà Oolong
    ('d0000000-0000-0000-0000-000000000002', 'TIER_1_3', 1, 3, 110000.00, 31.25),
    ('d0000000-0000-0000-0000-000000000002', 'TIER_4_6', 4, 6, 98000.00, 38.75),
    ('d0000000-0000-0000-0000-000000000002', 'TIER_7_PLUS', 7, NULL, 85000.00, 46.88),
    -- Nước Mắm Phú Quốc
    ('d0000000-0000-0000-0000-000000000003', 'TIER_1_5', 1, 5, 360000.00, 25.00),
    ('d0000000-0000-0000-0000-000000000003', 'TIER_6_15', 6, 15, 320000.00, 33.33),
    ('d0000000-0000-0000-0000-000000000003', 'TIER_16_PLUS', 16, NULL, 280000.00, 41.67),
    -- Sữa Hạt Macca
    ('d0000000-0000-0000-0000-000000000004', 'TIER_1_5', 1, 5, 510000.00, 25.00),
    ('d0000000-0000-0000-0000-000000000004', 'TIER_6_15', 6, 15, 460000.00, 32.35),
    ('d0000000-0000-0000-0000-000000000004', 'TIER_16_PLUS', 16, NULL, 410000.00, 39.71),
    -- Dầu Gội Bồ Kết
    ('d0000000-0000-0000-0000-000000000005', 'TIER_1_3', 1, 3, 1800000.00, 25.00),
    ('d0000000-0000-0000-0000-000000000005', 'TIER_4_10', 4, 10, 1600000.00, 33.33),
    ('d0000000-0000-0000-0000-000000000005', 'TIER_11_PLUS', 11, NULL, 1400000.00, 41.67)
ON CONFLICT (product_id, min_qty) DO NOTHING;

-- 7. SEED BRANCH INVENTORIES
INSERT INTO branch_inventory (branch_id, product_id, available_qty, reserved_qty, safety_stock)
VALUES
    -- Kho Tân Bình HCM
    ('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 500, 15, 20),
    ('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 380, 10, 20),
    ('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 200, 8, 15),
    ('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000004', 180, 5, 15),
    ('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000005', 90, 2, 10),
    -- Kho Cầu Giấy HN
    ('c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 320, 10, 15),
    ('c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 240, 5, 15),
    ('c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 120, 4, 10),
    ('c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000004', 150, 6, 10),
    ('c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000005', 65, 0, 10),
    -- Kho Hải Châu DN
    ('c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 180, 0, 10),
    ('c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002', 140, 0, 10),
    ('c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 85, 0, 5),
    ('c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004', 90, 0, 5),
    ('c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000005', 40, 0, 5)
ON CONFLICT (branch_id, product_id) DO NOTHING;

-- 8. SEED SAMPLE ORDERS
INSERT INTO orders (
    id, order_code, user_id, branch_id, order_type, order_status,
    payment_method, payment_status, shipping_provider, tracking_number,
    subtotal_amount, discount_amount, shipping_fee, tax_amount, total_amount,
    recipient_name, recipient_phone, shipping_postal_code,
    shipping_address_line, shipping_address_detail, order_note, created_at
) VALUES
    (
        'e0000000-0000-0000-0000-000000000001',
        'ORD-20261012-0001',
        'a0000000-0000-0000-0000-000000000005',
        'c0000000-0000-0000-0000-000000000001',
        'WHOLESALE',
        'PROCESSING',
        'VIETQR',
        'PAID',
        'GHN',
        'GHN-88291039VN',
        2050000.00,
        150000.00,
        45000.00,
        0.00,
        1945000.00,
        'Nguyễn Văn Thu Mua WinMart',
        '0904567890',
        '700000',
        'Kho Tổng WinCommerce Sóng Thần, TP. Dĩ An, Tỉnh Bình Dương',
        'Cổng B - Giao giờ hành chính',
        'Đơn nhập sỉ bổ sung định kỳ tuần 42',
        NOW() - INTERVAL '2 days'
    ),
    (
        'e0000000-0000-0000-0000-000000000002',
        'SMP-20261013-0002',
        'a0000000-0000-0000-0000-000000000005',
        'c0000000-0000-0000-0000-000000000001',
        'SAMPLE',
        'CONFIRMED',
        'VIETQR',
        'PAID',
        'AHAMOVE',
        'AHA-99381742',
        0.00,
        0.00,
        35000.00,
        0.00,
        35000.00,
        'Nguyễn Văn Thu Mua WinMart',
        '0904567890',
        '700000',
        'Kho Tổng WinCommerce Sóng Thần, TP. Dĩ An, Tỉnh Bình Dương',
        'Giao phòng QC mẫu sản phẩm mới',
        'Mẫu thử trà Ô long và Cà phê Espresso mới xuất xưởng',
        NOW() - INTERVAL '4 hours'
    )
ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (order_id, product_id, sku_snapshot, product_name_snap, unit_label_snap, applied_tier_code, quantity, unit_price, line_total)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'CF-ESP-01', 'Cà Phê Hòa Tan Espresso Dark Roast UMEGO', 'Hộp (20 gói)', 'TIER_7_PLUS', 10, 65000.00, 650000.00),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000004', 'MLK-MAC-04', 'Sữa Hạt Macca Hạnh Nhân Nguyên Chất Không Đường', 'Thùng (24 lốc 4 hộp)', 'TIER_1_5', 2, 510000.00, 1020000.00),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'FS-PQ-03', 'Nước Mắm Cốt Cá Cơm Phú Quốc 40 Độ Đạm', 'Thùng (12 chai 500ml)', 'TIER_1_5', 1, 360000.00, 360000.00),
    ('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'TEA-OOL-02', 'Trà Xanh Oolong Thượng Hạng Mộc Châu', 'Lon (100g)', 'SAMPLE_FREE', 1, 0.00, 0.00)
ON CONFLICT DO NOTHING;

-- 9. SEED BROADCAST ANNOUNCEMENTS
INSERT INTO announcements (id, title, content_html, category_badge, is_pinned, is_published, created_by, published_at)
VALUES (
    'f0000000-0000-0000-0000-000000000001',
    'Thông báo biểu phí sỉ ưu đãi Quý 4/2026 và chính sách mẫu thử miễn phí',
    '<p>Kính gửi Quý Đối Tác B2B,</p><p>Hệ thống UMEGO chính thức áp dụng khung chiết khấu sỉ mới lên đến <strong>45%</strong> cho các đơn hàng đạt hạn mức Tier 3. Đối với đối tác mới đăng ký, miễn phí 100% mẫu thử (chỉ thanh toán cước vận chuyển hãng).</p>',
    'CHÍNH_SÁCH',
    TRUE,
    TRUE,
    'a0000000-0000-0000-0000-000000000001',
    NOW()
) ON CONFLICT (id) DO NOTHING;
