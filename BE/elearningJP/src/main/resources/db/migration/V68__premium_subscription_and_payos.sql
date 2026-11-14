-- =============================================================================
-- V68: Hệ thống Hội viên PRO (Premium), Token Quota AI, Coupon và Thanh toán PayOS VietQR
-- -----------------------------------------------------------------------------
-- 1. Bổ sung cờ is_premium vào bảng courses và exams (cho phép Admin/Teacher phân loại).
-- 2. Tạo bảng subscription_plans (4 gói: FREE, PRO_1M, PRO_1Y, PRO_LIFETIME).
-- 3. Tạo bảng coupons (quản lý mã giảm giá sự kiện/sinh nhật).
-- 4. Tạo bảng user_subscriptions (theo dõi trạng thái PRO & thời hạn của học viên).
-- 5. Tạo bảng user_token_wallets (ví token dùng thử 3000 token + 500 token/ngày).
-- 6. Tạo bảng payment_orders (quản lý đơn hàng VietQR PayOS).
-- =============================================================================

-- 1. Cờ Premium cho Khóa học và Đề thi
ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Bảng cấu hình gói hội viên
CREATE TABLE IF NOT EXISTS subscription_plans (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_vnd INT NOT NULL,
    original_price_vnd INT,
    duration_days INT, -- NULL nếu là FREE hoặc PRO_LIFETIME
    monthly_tokens INT NOT NULL DEFAULT 0,
    max_practice_words INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO subscription_plans (id, name, price_vnd, original_price_vnd, duration_days, monthly_tokens, max_practice_words) VALUES
('FREE', 'Miễn phí', 0, 0, NULL, 0, 10),
('PRO_1M', 'PRO 1 Tháng', 99000, 129000, 30, 50000, 9999),
('PRO_1Y', 'PRO 1 Năm', 699000, 1188000, 365, 500000, 9999),
('PRO_LIFETIME', 'PRO Trọn Đời', 1299000, 2499000, NULL, 100000, 9999)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    price_vnd = EXCLUDED.price_vnd,
    original_price_vnd = EXCLUDED.original_price_vnd,
    duration_days = EXCLUDED.duration_days,
    max_practice_words = EXCLUDED.max_practice_words;

-- 3. Bảng mã giảm giá / Voucher sự kiện
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- 'PERCENTAGE' hoặc 'FIXED'
    discount_value INT NOT NULL,
    max_discount_amount INT,
    min_order_amount INT NOT NULL DEFAULT 0,
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons(code);

-- Seed mã giảm giá mẫu chào mừng
INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, is_active)
VALUES ('ELEARNING2026', 'PERCENTAGE', 10, 0, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 4. Bảng theo dõi gói dịch vụ của người dùng (user_id kiểu BIGINT khớp với users.id)
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id VARCHAR(50) NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'EXPIRED', 'CANCELLED'
    start_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_sub_lookup ON user_subscriptions(user_id, status);

-- 5. Bảng ví Token AI hội thoại (user_id kiểu BIGINT khớp với users.id)
CREATE TABLE IF NOT EXISTS user_token_wallets (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    trial_balance INT NOT NULL DEFAULT 3000,
    daily_free_balance INT NOT NULL DEFAULT 500,
    subscription_balance INT NOT NULL DEFAULT 0,
    last_daily_reset DATE NOT NULL DEFAULT CURRENT_DATE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng đơn hàng thanh toán PayOS VietQR
CREATE TABLE IF NOT EXISTS payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code BIGINT UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id VARCHAR(50) NOT NULL REFERENCES subscription_plans(id),
    coupon_id UUID REFERENCES coupons(id) ON DELETE SET NULL,
    original_amount_vnd INT NOT NULL,
    discount_amount_vnd INT NOT NULL DEFAULT 0,
    amount_vnd INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'CANCELLED', 'EXPIRED'
    checkout_url TEXT,
    qr_code TEXT,
    payos_payment_link_id VARCHAR(100),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payment_order_code ON payment_orders(order_code);
CREATE INDEX IF NOT EXISTS idx_payment_order_user ON payment_orders(user_id, status);

-- 7. Backfill dữ liệu ví token & gói Free cho các user hiện có
INSERT INTO user_token_wallets (user_id, trial_balance, daily_free_balance, last_daily_reset)
SELECT id, 3000, 500, CURRENT_DATE
FROM users
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_subscriptions (user_id, plan_id, status, start_at)
SELECT id, 'FREE', 'ACTIVE', CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_subscriptions s WHERE s.user_id = u.id AND s.status = 'ACTIVE'
);
