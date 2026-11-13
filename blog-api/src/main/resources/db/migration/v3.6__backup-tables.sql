CREATE TABLE t_backup (
  id CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  version VARCHAR(100) NULL,
  build VARCHAR(32) NULL,
  operator_id BIGINT NULL,
  created_at VARCHAR(40) NOT NULL,
  protected_backup BOOLEAN NOT NULL DEFAULT FALSE,
  manifest_sha256 CHAR(64) NULL,
  total_bytes BIGINT NOT NULL DEFAULT 0,
  source_operation_id CHAR(36) NULL,
  metadata LONGTEXT NOT NULL,
  INDEX idx_backup_created (created_at)
);

CREATE TABLE t_backup_operation (
  id CHAR(36) NOT NULL PRIMARY KEY,
  operation_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  operator_id BIGINT NULL,
  created_at VARCHAR(40) NOT NULL,
  finished_at VARCHAR(40) NULL,
  backup_id CHAR(36) NULL,
  protection_backup_id CHAR(36) NULL,
  metadata LONGTEXT NOT NULL,
  INDEX idx_backup_operation_created (created_at),
  INDEX idx_backup_operation_backup (backup_id)
);
