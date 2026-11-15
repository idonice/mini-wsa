CREATE TABLE events (
    event_id        VARCHAR(100)  PRIMARY KEY,
    timestamp       TIMESTAMPTZ   NOT NULL,
    config_id       BIGINT        NOT NULL,
    policy_id       VARCHAR(100),
    client_ip       VARCHAR(45)   NOT NULL,
    hostname        VARCHAR(255),
    path            VARCHAR(2048) NOT NULL,
    method          VARCHAR(10),
    status_code     INTEGER,
    user_agent      VARCHAR(1024),
    action          VARCHAR(20)   NOT NULL,
    request_size    BIGINT,
    response_size   BIGINT,
    rule_id         VARCHAR(50),
    rule_name       VARCHAR(100),
    rule_message    VARCHAR(500),
    rule_severity   VARCHAR(20)   NOT NULL,
    rule_category   VARCHAR(30)   NOT NULL,
    geo_country     VARCHAR(2),
    geo_city        VARCHAR(100),
    attack_type     VARCHAR(50)   NOT NULL,
    threat_score    INTEGER       NOT NULL,
    received_at     TIMESTAMPTZ   NOT NULL
);

-- Repeat-offender check on every ingest: same IP within the last 10 minutes
CREATE INDEX idx_events_client_ip_time ON events (client_ip, timestamp);

-- Stats and samples filtered by configId + time range
CREATE INDEX idx_events_config_time ON events (config_id, timestamp);

-- Stats and samples without configId, and samples sorted by newest
CREATE INDEX idx_events_time ON events (timestamp);
