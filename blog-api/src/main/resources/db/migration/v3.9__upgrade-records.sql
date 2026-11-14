drop table if exists t_backup_operation;

alter table t_backup
    drop column metadata,
    drop column operator_id,
    add column location varchar(1024) null after manifest_sha256,
    add column scope varchar(500) null,
    add column restore_verified_at varchar(40) null;

alter table t_upgrade_package
    add column display_name varchar(255) null after original_name,
    add column note varchar(500) null after display_name,
    add column location varchar(1024) not null default '' after sha256,
    drop column operator_id,
    drop column metadata;

create table t_upgrade_record
(
    id                           char(36)     not null primary key,
    package_id                   char(36)     not null,
    package_name                 varchar(255) not null,
    package_version              varchar(100) not null,
    package_build                bigint       not null,
    package_sha256               char(64)     not null,
    package_path                 varchar(1024) not null,
    old_version                  varchar(100) null,
    old_build                    bigint       null,
    status                       varchar(32)  not null,
    backup_enabled               boolean      not null default true,
    backup_timeout_seconds       int          not null default 600,
    health_timeout_seconds       int          not null default 90,
    note                         varchar(500) null,
    delete_package_after_upgrade boolean      not null default false,
    release_path                 varchar(1024) null,
    backup_path                  varchar(1024) null,
    backup_bytes                 bigint       null,
    backup_sha256                char(64)     null,
    backup_completed_at          varchar(40)  null,
    message                      varchar(1000) null,
    created_at                   varchar(40)  not null,
    started_at                   varchar(40)  null,
    finished_at                  varchar(40)  null,
    index idx_upgrade_record_created (created_at),
    index idx_upgrade_record_package (package_id)
);

create table t_upgrade_path_mapping
(
    id        bigint       not null auto_increment primary key,
    record_id char(36)     not null,
    name      varchar(32)  not null,
    mode      varchar(16)  not null,
    source    varchar(255) not null,
    target    varchar(512) not null,
    index idx_upgrade_path_mapping_record (record_id),
    unique key uk_upgrade_path_mapping_name (record_id, name)
);

create table t_upgrade_setting
(
    id                     tinyint     not null primary key,
    backup_enabled         boolean     not null,
    backup_timeout_seconds int         not null,
    health_timeout_seconds int         not null,
    note                   varchar(500) not null
);

insert into t_upgrade_setting (id, backup_enabled, backup_timeout_seconds, health_timeout_seconds, note)
values (1, true, 600, 90, '');

alter table t_release_mapping drop column operator_id;
