-- ==============================================================================
-- V4__chat_indexes.sql
-- Optimizes high-throughput Real-time WebSocket Chat queries
-- ==============================================================================

-- 1. Index for room message history with cursor-based seek (ESR: room_id -> id DESC)
CREATE INDEX IF NOT EXISTS idx_chat_messages_room_id_desc
    ON chat_messages (room_id, id DESC);

-- 2. Partial index for fast unread message badge count
CREATE INDEX IF NOT EXISTS idx_chat_messages_unread
    ON chat_messages (room_id, sender_id, is_read)
    WHERE is_read = FALSE;

-- 3. Composite index for Admin room listing filtered by status and sorted by latest message
CREATE INDEX IF NOT EXISTS idx_chat_rooms_status_last_message
    ON chat_rooms (status, last_message_at DESC);
