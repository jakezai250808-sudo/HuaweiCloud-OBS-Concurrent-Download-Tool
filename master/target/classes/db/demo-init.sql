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
