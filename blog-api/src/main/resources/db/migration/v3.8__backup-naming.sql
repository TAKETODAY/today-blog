ALTER TABLE t_backup
  CHANGE COLUMN protected_snapshot protected_backup BOOLEAN NOT NULL DEFAULT FALSE,
  RENAME INDEX idx_app_snapshot_created TO idx_backup_created;
ALTER TABLE t_app_operation
  CHANGE COLUMN snapshot_id backup_id CHAR(36) NULL,
  CHANGE COLUMN protection_snapshot_id protection_backup_id CHAR(36) NULL,
  RENAME INDEX idx_app_operation_snapshot TO idx_app_operation_backup;

UPDATE t_app_operation
SET operation_type = REPLACE(operation_type, 'SNAPSHOT_', 'BACKUP_')
WHERE operation_type IN ('SNAPSHOT_CREATE', 'SNAPSHOT_RESTORE', 'SNAPSHOT_DELETE', 'SNAPSHOT_PROTECT');
