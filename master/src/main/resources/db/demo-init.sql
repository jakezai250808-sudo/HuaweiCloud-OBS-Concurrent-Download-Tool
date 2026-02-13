CREATE TABLE IF NOT EXISTS obs_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(128) NOT NULL,
    access_key VARCHAR(256) NOT NULL,
    secret_key VARCHAR(256) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
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
