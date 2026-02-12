CREATE DATABASE IF NOT EXISTS obsdl DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE obsdl;

CREATE TABLE IF NOT EXISTS obs_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(128) NOT NULL,
    access_key VARCHAR(256) NOT NULL,
    secret_key VARCHAR(256) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_name (account_name)
);

CREATE TABLE IF NOT EXISTS download_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT,
    bucket VARCHAR(128) NOT NULL,
    concurrency INT NOT NULL DEFAULT 4,
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
