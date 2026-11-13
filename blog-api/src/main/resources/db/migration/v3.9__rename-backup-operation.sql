RENAME TABLE t_app_operation TO t_backup_operation;

ALTER TABLE t_backup_operation
  RENAME INDEX idx_app_operation_created TO idx_backup_operation_created,
  RENAME INDEX idx_app_operation_backup TO idx_backup_operation_backup;
