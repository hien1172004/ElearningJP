-- ============================================================
-- V39: Sửa dữ liệu bình luận bị lệch do các lỗi cũ
-- ============================================================
-- Mục đích & lý do:
--   1. Xoá reply cha trước đây chỉ xoá mềm đúng 1 dòng; reply con vẫn còn và
--      FE đẩy chúng lên cấp gốc. Code mới xoá cả cây con — migration này dọn
--      những reply "mồ côi" đã phát sinh (cha/ông đã bị xoá mềm).
--   2. Bộ đếm like/dislike/reply có thể lệch: sửa/xoá comment từng ghi đè cả
--      các cột count (entity chưa có @DynamicUpdate), addReply ghi đè
--      reply_count, xoá reply cha chỉ trừ 1. Tính lại toàn bộ từ dữ liệu gốc.
-- Chỉ là UPDATE dữ liệu, không đổi schema; chạy lại nhiều lần vẫn cho cùng kết quả.

-- 1. Xoá mềm mọi reply còn sống có tổ tiên đã bị xoá mềm
WITH RECURSIVE orphan AS (
    SELECT r.id
    FROM comment_replies r
    JOIN comment_replies p ON p.id = r.parent_reply_id
    WHERE r.deleted = FALSE AND p.deleted = TRUE
    UNION
    SELECT r.id
    FROM comment_replies r
    JOIN orphan o ON r.parent_reply_id = o.id
    WHERE r.deleted = FALSE
)
UPDATE comment_replies
SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
WHERE id IN (SELECT id FROM orphan);

-- 2. Tính lại bộ đếm của comment
UPDATE comments c
SET reply_count   = (SELECT COUNT(*) FROM comment_replies r WHERE r.comment_id = c.id AND r.deleted = FALSE),
    like_count    = (SELECT COUNT(*) FROM comment_likes l WHERE l.comment_id = c.id),
    dislike_count = (SELECT COUNT(*) FROM comment_dislikes d WHERE d.comment_id = c.id);

-- 3. Tính lại bộ đếm của reply
UPDATE comment_replies r
SET like_count    = (SELECT COUNT(*) FROM comment_reply_likes l WHERE l.reply_id = r.id),
    dislike_count = (SELECT COUNT(*) FROM comment_reply_dislikes d WHERE d.reply_id = r.id);
