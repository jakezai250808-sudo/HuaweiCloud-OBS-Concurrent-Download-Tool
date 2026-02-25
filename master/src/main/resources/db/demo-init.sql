CREATE TABLE IF NOT EXISTS obs_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(128) NOT NULL,
    access_key VARCHAR(256) NOT NULL,
    secret_key VARCHAR(256) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_name)
);

CREATE TABLE IF NOT EXISTS download_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT,
    bucket VARCHAR(128) NOT NULL,
    concurrency INT NOT NULL DEFAULT 4,
    total_objects INT NOT NULL DEFAULT 0,
    done_objects INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_download_task_account FOREIGN KEY (account_id) REFERENCES obs_account(id)
);

CREATE INDEX IF NOT EXISTS idx_task_status ON download_task (status);

CREATE TABLE IF NOT EXISTS task_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    size BIGINT,
    status VARCHAR(32) NOT NULL,
    leased_by VARCHAR(128),
    etag VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_object_task FOREIGN KEY (task_id) REFERENCES download_task(id),
    CONSTRAINT uk_task_id_object_key UNIQUE (task_id, object_key)
);

CREATE INDEX IF NOT EXISTS idx_task_id_status ON task_object (task_id, status);

CREATE TABLE IF NOT EXISTS worker_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_name VARCHAR(128) NOT NULL,
    host VARCHAR(128) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_worker_host_port UNIQUE (host, port)
);


CREATE TABLE IF NOT EXISTS api_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(128) NOT NULL,
    name VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_token_token UNIQUE (token)
);

CREATE TABLE IF NOT EXISTS obs_mock_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bucket VARCHAR(128) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    etag VARCHAR(128),
    storage_class VARCHAR(64),
    CONSTRAINT uk_obs_mock_bucket_object UNIQUE (bucket, object_key)
);

-- Seed demo data for local API debugging.
MERGE INTO obs_account (id, account_name, access_key, secret_key, endpoint, bucket, created_at, updated_at)
KEY (id) VALUES
    (1, 'demo-account-1', 'ak-demo-1', 'sk-demo-1', 'obs-cn-north-4.myhuaweicloud.com', 'demo-bucket', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'demo-account-2', 'ak-demo-2', 'sk-demo-2', 'obs-ap-southeast-1.myhuaweicloud.com', 'demo-bucket-2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO download_task (id, account_id, bucket, concurrency, total_objects, done_objects, status, created_at, updated_at)
KEY (id) VALUES
    (1001, 1, 'demo-bucket', 4, 2, 0, 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1002, 2, 'demo-bucket', 4, 1, 1, 'DONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO task_object (id, task_id, object_key, size, status, leased_by, etag, created_at, updated_at)
KEY (id) VALUES
    (2001, 1001, 'incoming/string/2026/01/events-0001.json', 0, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2002, 1001, 'incoming/string/2026/01/events-0002.json', 0, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2003, 1002, 'incoming/string/2026/01/summary.csv', 0, 'DONE', 'worker-demo', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO obs_mock_object (id, bucket, object_key, size, last_modified, etag, storage_class)
KEY (id) VALUES
    (3001, 'demo-bucket', 'photos/2025/01/a.jpg', 1024, TIMESTAMP '2026-02-21 10:00:00', 'etag-3001', 'STANDARD'),
    (3002, 'demo-bucket', 'photos/2025/02/b.jpg', 2048, TIMESTAMP '2026-02-21 10:01:00', 'etag-3002', 'STANDARD'),
    (3003, 'demo-bucket', 'photos/readme.txt', 128, TIMESTAMP '2026-02-21 10:02:00', 'etag-3003', 'STANDARD'),
    (3004, 'demo-bucket', 'docs/specs/v1/design.pdf', 4096, TIMESTAMP '2026-02-21 10:03:00', 'etag-3004', 'STANDARD'),
    (3005, 'demo-bucket', 'docs/specs/v1/api.yaml', 512, TIMESTAMP '2026-02-21 10:04:00', 'etag-3005', 'STANDARD'),
    (3006, 'demo-bucket', 'docs/specs/v2/api.yaml', 768, TIMESTAMP '2026-02-21 10:05:00', 'etag-3006', 'STANDARD'),
    (3007, 'demo-bucket', 'root.txt', 64, TIMESTAMP '2026-02-21 10:06:00', 'etag-3007', 'STANDARD_IA'),
    (3008, 'demo-bucket-2', 'reports/2026/summary.csv', 900, TIMESTAMP '2026-02-21 10:07:00', 'etag-3008', 'STANDARD'),
    (3009, 'demo-bucket-2', 'reports/readme.md', 80, TIMESTAMP '2026-02-21 10:08:00', 'etag-3009', 'STANDARD'),
    (3010, 'demo-bucket-logs', 'app/2026/02/21/log-0001.gz', 1200, TIMESTAMP '2026-02-21 10:09:00', 'etag-3010', 'WARM'),
    (3011, 'demo-bucket-logs', 'app/2026/02/21/log-0002.gz', 1400, TIMESTAMP '2026-02-21 10:10:00', 'etag-3011', 'WARM');

MERGE INTO api_token (id, token, name, enabled, created_at, updated_at)
KEY (id) VALUES
    (1, 'change_me', 'default', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

