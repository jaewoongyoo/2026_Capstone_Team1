ALTER TABLE dashboard ADD COLUMN share_token VARCHAR(36);
ALTER TABLE dashboard ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX idx_dashboard_share_token ON dashboard(share_token);
