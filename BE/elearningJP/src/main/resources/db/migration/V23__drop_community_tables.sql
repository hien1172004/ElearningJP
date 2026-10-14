-- ============================================================
-- V23: Drop community tables (discussions + replies + likes + reports)
-- ============================================================
-- Sau khi xóa module Community, toàn bộ các bảng discussions
-- cũng bị gỡ khỏi database. Thứ tự drop theo FK phụ thuộc:
--   content_reports → discussion_reply_likes → discussion_replies
--   → discussion_likes → discussions

DROP TABLE IF EXISTS content_reports CASCADE;
DROP TABLE IF EXISTS discussion_reply_likes CASCADE;
DROP TABLE IF EXISTS discussion_replies CASCADE;
DROP TABLE IF EXISTS discussion_likes CASCADE;
DROP TABLE IF EXISTS discussions CASCADE;
