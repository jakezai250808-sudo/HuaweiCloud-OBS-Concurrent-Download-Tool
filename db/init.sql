CREATE DATABASE IF NOT EXISTS obsdl DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE obsdl;

CREATE TABLE IF NOT EXISTS obs_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(128) NOT NULL,
    access_key VARCHAR(256) NOT NULL,
    secret_key VARCHAR(256) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_name (account_name)
);

CREATE TABLE IF NOT EXISTS download_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT,
    bucket VARCHAR(128) NOT NULL,
    concurrency INT NOT NULL DEFAULT 4,
    total_objects INT NOT NULL DEFAULT 0,
    done_objects INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_task_status (status),
    CONSTRAINT fk_download_task_account FOREIGN KEY (account_id) REFERENCES obs_account(id)
);

CREATE TABLE IF NOT EXISTS task_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    size BIGINT,
    status VARCHAR(32) NOT NULL,
    leased_by VARCHAR(128),
    etag VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_task_id_status (task_id, status),
    UNIQUE KEY uk_task_id_object_key (task_id, object_key),
    CONSTRAINT fk_task_object_task FOREIGN KEY (task_id) REFERENCES download_task(id)
);

CREATE TABLE IF NOT EXISTS worker_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_name VARCHAR(128) NOT NULL,
    host VARCHAR(128) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_worker_host_port (host, port)
);

CREATE TABLE IF NOT EXISTS obs_mock_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bucket VARCHAR(128) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    last_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    etag VARCHAR(128),
    storage_class VARCHAR(64),
    UNIQUE KEY uk_obs_mock_bucket_object (bucket, object_key),
    KEY idx_obs_mock_bucket_key (bucket, object_key)
);


CREATE TABLE IF NOT EXISTS api_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(128) NOT NULL,
    name VARCHAR(64),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_api_token_token (token)
);

INSERT INTO api_token(token, name, enabled)
VALUES ('change_me', 'default', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
