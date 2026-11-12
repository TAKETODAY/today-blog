CREATE TABLE t_app_snapshot (
  id CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  version VARCHAR(100) NULL,
  build VARCHAR(32) NULL,
  operator_id BIGINT NULL,
  created_at VARCHAR(40) NOT NULL,
  protected_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
  manifest_sha256 CHAR(64) NULL,
  total_bytes BIGINT NOT NULL DEFAULT 0,
  source_operation_id CHAR(36) NULL,
  metadata LONGTEXT NOT NULL,
  INDEX idx_app_snapshot_created (created_at)
);

CREATE TABLE t_app_operation (
  id CHAR(36) NOT NULL PRIMARY KEY,
  operation_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  operator_id BIGINT NULL,
  created_at VARCHAR(40) NOT NULL,
  finished_at VARCHAR(40) NULL,
  snapshot_id CHAR(36) NULL,
  protection_snapshot_id CHAR(36) NULL,
  metadata LONGTEXT NOT NULL,
  INDEX idx_app_operation_created (created_at),
  INDEX idx_app_operation_snapshot (snapshot_id)
);
