-- Table for website metadata
CREATE TABLE websites (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id TEXT NOT NULL, -- Simplified for now, should be UUID in real app
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table for subscriptions (FCM Tokens)
CREATE TABLE subscribers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    website_id TEXT REFERENCES websites(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(website_id, fcm_token)
);

-- Table for notification logs
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    website_id TEXT REFERENCES websites(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE websites ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscribers ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Policies for subscribers (Allow anon registration from Android App)
CREATE POLICY "Allow public registration" ON subscribers
FOR INSERT WITH CHECK (true);

-- Policies for websites (Read-only for public)
CREATE POLICY "Public read websites" ON websites
FOR SELECT USING (true);

-- Policies for website management (Allow owner to manage)
CREATE POLICY "Owners can manage websites" ON websites
FOR ALL USING (true); -- Simplified for this demo/applet environment
