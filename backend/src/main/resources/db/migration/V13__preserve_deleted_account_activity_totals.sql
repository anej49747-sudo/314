-- #51/#20: keep anonymous usage totals without keeping deleted account identities.
-- Period-level distinct-user counts must be preserved separately from daily counts.
CREATE TABLE deleted_account_activity_totals (
    period_type VARCHAR(8) NOT NULL CHECK (period_type IN ('DAY', 'WEEK', 'MONTH')),
    period_start DATE NOT NULL,
    metric VARCHAR(24) NOT NULL CHECK (metric IN (
        'LOGINS', 'UNIQUE_USERS', 'NEW_ACCOUNTS', 'REVIEWS')),
    total BIGINT NOT NULL CHECK (total >= 0),
    PRIMARY KEY (period_type, period_start, metric)
);
