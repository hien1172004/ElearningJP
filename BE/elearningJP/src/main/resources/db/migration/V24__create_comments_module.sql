-- ============================================================
-- V24: Create comments module (bình luận bài học + tra cứu)
-- ============================================================
-- Một bảng comments duy nhất phục vụ nhiều loại đối tượng
-- (LESSON / VOCABULARY / KANJI / GRAMMAR) thông qua enum
-- target_type + target_id. Reply lồng nhau qua parent_reply_id.
-- Có cả like + dislike (frontend hiện chỉ hiển thị like).

-- 1. Bảng comments
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE',
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comments_target ON comments(target_type, target_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_comments_user ON comments(user_id) WHERE deleted = FALSE;

-- 2. Like comment
CREATE TABLE IF NOT EXISTS comment_likes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, comment_id)
);
CREATE INDEX IF NOT EXISTS idx_comment_likes_comment ON comment_likes(comment_id);

-- 3. Dislike comment
CREATE TABLE IF NOT EXISTS comment_dislikes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, comment_id)
);
CREATE INDEX IF NOT EXISTS idx_comment_dislikes_comment ON comment_dislikes(comment_id);

-- 4. Reply cho comment (cho phép lồng nhau qua parent_reply_id)
CREATE TABLE IF NOT EXISTS comment_replies (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    parent_reply_id BIGINT REFERENCES comment_replies(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE',
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comment_replies_comment ON comment_replies(comment_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_comment_replies_parent ON comment_replies(parent_reply_id) WHERE deleted = FALSE;

-- 5. Like reply
CREATE TABLE IF NOT EXISTS comment_reply_likes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reply_id BIGINT NOT NULL REFERENCES comment_replies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, reply_id)
);
CREATE INDEX IF NOT EXISTS idx_comment_reply_likes_reply ON comment_reply_likes(reply_id);

-- 6. Dislike reply
CREATE TABLE IF NOT EXISTS comment_reply_dislikes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reply_id BIGINT NOT NULL REFERENCES comment_replies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, reply_id)
);
CREATE INDEX IF NOT EXISTS idx_comment_reply_dislikes_reply ON comment_reply_dislikes(reply_id);
