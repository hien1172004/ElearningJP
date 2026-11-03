-- ============================================================================
-- Migration: V40__create_notebook_community_tables.sql
-- Mục đích: Bảng theo dõi lượt Thích (Likes) và Lịch sử xem (Views) của Sổ tay Cộng đồng
-- ============================================================================

CREATE TABLE IF NOT EXISTS notebook_likes (
    id BIGSERIAL PRIMARY KEY,
    notebook_id BIGINT NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notebook_user_like UNIQUE (notebook_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_notebook_likes_user ON notebook_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_notebook_likes_notebook ON notebook_likes(notebook_id);

CREATE TABLE IF NOT EXISTS notebook_views (
    id BIGSERIAL PRIMARY KEY,
    notebook_id BIGINT NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notebook_user_view UNIQUE (notebook_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_notebook_views_user ON notebook_views(user_id);
CREATE INDEX IF NOT EXISTS idx_notebook_views_notebook ON notebook_views(notebook_id);

