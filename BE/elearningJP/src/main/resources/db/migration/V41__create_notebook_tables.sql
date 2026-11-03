-- =============================================================================
-- Migration V38: Tạo bảng nhóm sổ tay (notebook_groups) và sổ tay (notebooks)
-- =============================================================================

CREATE TABLE IF NOT EXISTS notebook_groups (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notebook_groups_user_id ON notebook_groups(user_id) WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS notebooks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT REFERENCES notebook_groups(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    cover_color VARCHAR(20),
    cover_icon VARCHAR(50),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    share_code VARCHAR(10) UNIQUE,
    jlpt_level VARCHAR(2),
    word_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    download_count INT NOT NULL DEFAULT 0,
    sr_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    source_notebook_id BIGINT REFERENCES notebooks(id) ON DELETE SET NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notebooks_user_id ON notebooks(user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebooks_group_id ON notebooks(group_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebooks_public ON notebooks(is_public, created_at DESC) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebooks_share_code ON notebooks(share_code) WHERE deleted = FALSE;

