-- 附件的 uri 是公开 URL，不参与迁移。
update attachment
set location = substring(location, 2)
where location like '/upload/%';

-- 升级包固定存放为 <包目录>/package.zip，兼容曾配置过的升级状态根目录。
update t_upgrade_package
set location = substring_index(location, '/', -2)
where location like '/%/package.zip';

update t_upgrade_record
set package_path = substring_index(package_path, '/', -2)
where package_path like '/%/package.zip';

-- 发行目录以安装根目录为基准，数据库备份以配置的备份根目录为基准。
update t_upgrade_record
set release_path = substring_index(release_path, '/', -2)
where release_path like '/%/releases/%';

update t_upgrade_record
set backup_path = substring_index(backup_path, '/', -2)
where backup_path like '/%/database.sql.gz';

update t_backup
set location = substring_index(location, '/', -2)
where location like '/%/database.sql.gz';

-- 映射目标以服务器文件系统根目录为基准，实际发布仍受白名单限制。
update t_release_mapping
set target = substring(target, 2)
where target like '/%';

update t_upgrade_path_mapping
set target = substring(target, 2)
where target like '/%';
