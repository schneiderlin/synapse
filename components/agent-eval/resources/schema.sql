-- agent-eval database schema
-- To initialize: sqlite3 databases/agent-eval.db < resources/schema.sql

CREATE TABLE IF NOT EXISTS llm_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    input TEXT NOT NULL,
    output TEXT,
    session_id TEXT NOT NULL DEFAULT 'dummy-session',
    extra TEXT,
    timestamp INTEGER NOT NULL
);
