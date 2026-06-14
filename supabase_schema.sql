-- ============================================================
-- GuardSMS — Supabase SQL Schema
-- Run this in your Supabase SQL Editor
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────
-- 1. SMS Messages (raw, auto-deleted after 24h)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sender          TEXT NOT NULL,
    body            TEXT NOT NULL,
    sender_name     TEXT,
    is_contact      BOOLEAN DEFAULT FALSE,
    extracted_links JSONB DEFAULT '[]',
    extracted_domains JSONB DEFAULT '[]',
    status          TEXT DEFAULT 'PENDING',
    threat_level    TEXT DEFAULT 'NONE',
    threat_reason   TEXT,
    is_redflagged   BOOLEAN DEFAULT FALSE,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    analyzed_at     TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for user queries and cleanup
CREATE INDEX IF NOT EXISTS idx_sms_messages_user_id ON sms_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_sms_messages_expires_at ON sms_messages(expires_at);
CREATE INDEX IF NOT EXISTS idx_sms_messages_received_at ON sms_messages(received_at DESC);

-- Auto-delete expired messages via pg_cron (schedule in Supabase dashboard)
-- SELECT cron.schedule('delete-expired-sms', '0 * * * *', $$DELETE FROM sms_messages WHERE expires_at < NOW()$$);

-- ─────────────────────────────────────────────
-- 2. Flagged Domains (hashed — community shared)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS flagged_domains (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    domain_hash     TEXT NOT NULL,           -- SHA-256 of domain
    domain          TEXT NOT NULL,           -- raw domain for display
    url_hash        TEXT,                    -- SHA-256 of full URL if provided
    url             TEXT,                    -- raw URL
    report_count    INT DEFAULT 1,
    threat_type     TEXT NOT NULL DEFAULT 'unknown',
    description     TEXT,
    is_verified     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_flagged_domains_hash ON flagged_domains(domain_hash);
CREATE INDEX IF NOT EXISTS idx_flagged_domains_report_count ON flagged_domains(report_count DESC);

-- ─────────────────────────────────────────────
-- 3. Flagged Messages (hashed — community shared)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS flagged_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    message_hash    TEXT NOT NULL UNIQUE,    -- SHA-256 of message body
    raw_preview     TEXT NOT NULL,           -- first 120 chars for context
    extracted_links JSONB DEFAULT '[]',
    extracted_domains JSONB DEFAULT '[]',
    report_count    INT DEFAULT 1,
    threat_type     TEXT NOT NULL DEFAULT 'unknown',
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_flagged_messages_hash ON flagged_messages(message_hash);

-- ─────────────────────────────────────────────
-- 4. Contacts (raw — per user, permanent)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS contacts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name              TEXT NOT NULL,
    phone             TEXT NOT NULL,
    phone_normalized  TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contacts_user_id ON contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_contacts_phone_norm ON contacts(user_id, phone_normalized);

-- ─────────────────────────────────────────────
-- 5. User Reports
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    report_type     TEXT NOT NULL,           -- 'domain' | 'url' | 'message' | 'sms'
    content         TEXT NOT NULL,           -- raw input from user
    content_hash    TEXT NOT NULL,           -- SHA-256 of content
    threat_type     TEXT NOT NULL DEFAULT 'unknown',
    description     TEXT,
    status          TEXT DEFAULT 'pending',  -- pending | reviewed | verified | dismissed
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_reports_user_id ON user_reports(user_id);

-- ─────────────────────────────────────────────
-- Row Level Security (RLS)
-- ─────────────────────────────────────────────

-- sms_messages: users see only their own
ALTER TABLE sms_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own messages" ON sms_messages
    FOR ALL USING (auth.uid() = user_id);

-- flagged_domains: readable by all authenticated users, writable by authenticated
ALTER TABLE flagged_domains ENABLE ROW LEVEL SECURITY;
CREATE POLICY "All users can read flagged domains" ON flagged_domains
    FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Authenticated users can insert flagged domains" ON flagged_domains
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Authenticated users can update flagged domains" ON flagged_domains
    FOR UPDATE USING (auth.role() = 'authenticated');

-- flagged_messages: readable by all, writable by authenticated
ALTER TABLE flagged_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "All users can read flagged messages" ON flagged_messages
    FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Authenticated users can insert flagged messages" ON flagged_messages
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Authenticated users can update flagged messages" ON flagged_messages
    FOR UPDATE USING (auth.role() = 'authenticated');

-- contacts: users see only their own
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own contacts" ON contacts
    FOR ALL USING (auth.uid() = user_id);

-- user_reports: users see only their own
ALTER TABLE user_reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own reports" ON user_reports
    FOR ALL USING (auth.uid() = user_id);

-- ─────────────────────────────────────────────
-- Seed: Known Philippine phishing domains
-- ─────────────────────────────────────────────
INSERT INTO flagged_domains (domain_hash, domain, threat_type, report_count, is_verified)
VALUES
    (encode(digest('gcash-verify.tk', 'sha256'), 'hex'), 'gcash-verify.tk', 'phishing', 42, true),
    (encode(digest('bdo-online-update.xyz', 'sha256'), 'hex'), 'bdo-online-update.xyz', 'phishing', 38, true),
    (encode(digest('ph-reward-claim.top', 'sha256'), 'hex'), 'ph-reward-claim.top', 'fraud', 29, true),
    (encode(digest('paymaya-promo.click', 'sha256'), 'hex'), 'paymaya-promo.click', 'phishing', 25, true),
    (encode(digest('load-reward-ph.ga', 'sha256'), 'hex'), 'load-reward-ph.ga', 'fraud', 19, false)
ON CONFLICT (domain_hash) DO NOTHING;
