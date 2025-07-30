CREATE INDEX IF NOT EXISTS idx_notes_user_expires_created ON notes (user_id, expires_at, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notes_id_user ON notes (id, user_id);
