-- Story #17: durable per-project view totals. Session-level deduplication is performed
-- by the application; no session IDs, IP addresses, or visitor identities are persisted.
CREATE TABLE project_view_counts (
    project_id BIGINT PRIMARY KEY REFERENCES interior_design_projects(id) ON DELETE CASCADE,
    view_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT project_view_counts_nonnegative CHECK (view_count >= 0)
);
