create table t_backup
(
    id                  bigint unsigned not null auto_increment primary key,
    name                varchar(200)  not null,
    status              varchar(32)   not null,
    version             varchar(100)  null,
    build               varchar(32)   null,
    created_at          datetime   not null,
    protected_backup    boolean       not null default false,
    manifest_sha256     char(64)      null,
    total_bytes         bigint        not null default 0,
    source_operation_id bigint unsigned null,
    location            varchar(1024) null,
    scope               varchar(500)  null,
    restore_verified_at datetime   null
);

create table t_release_mapping
(
    id         bigint unsigned not null auto_increment primary key,
    name       varchar(32)  not null,
    mode       varchar(16)  not null,
    source     varchar(255) not null,
    target     varchar(512) not null,
    created_at datetime  not null,
    updated_at datetime  not null
);

create table t_upgrade_package
(
    id            bigint unsigned not null auto_increment primary key,
    original_name varchar(255)  not null,
    display_name  varchar(255)  null,
    note          varchar(500)  null,
    status        varchar(32)   not null,
    version       varchar(100)  not null,
    build         bigint        not null,
    file_size     bigint        not null,
    sha256        char(64)      not null,
    entry_count   int           not null default 0,
    location      varchar(1024) not null,
    created_at    datetime   not null,
    consumed_at   datetime   null
);

create table t_upgrade_record
(
    id                           bigint unsigned not null auto_increment primary key,
    package_id                   bigint unsigned not null,
    package_name                 varchar(255)  not null,
    package_version              varchar(100)  not null,
    package_build                bigint        not null,
    package_sha256               char(64)      not null,
    package_path                 varchar(1024) not null,
    old_version                  varchar(100)  null,
    old_build                    bigint        null,
    status                       varchar(32)   not null,
    backup_enabled               boolean       not null default true,
    backup_timeout_seconds       int           not null default 600,
    health_timeout_seconds       int           not null default 90,
    note                         varchar(500)  null,
    delete_package_after_upgrade boolean       not null default false,
    release_path                 varchar(1024) null,
    backup_path                  varchar(1024) null,
    backup_bytes                 bigint        null,
    backup_sha256                char(64)      null,
    backup_completed_at          datetime   null,
    message                      varchar(1000) null,
    created_at                   datetime   not null,
    started_at                   datetime   null,
    finished_at                  datetime   null
);

create table t_upgrade_path_mapping
(
    id        bigint unsigned not null auto_increment primary key,
    record_id bigint unsigned not null,
    name      varchar(32)  not null,
    mode      varchar(16)  not null,
    source    varchar(255) not null,
    target    varchar(512) not null
);

create table t_upgrade_setting
(
    id                     tinyint      not null primary key,
    backup_enabled         boolean      not null,
    backup_timeout_seconds int          not null,
    health_timeout_seconds int          not null,
    note                   varchar(500) not null
);

insert into t_upgrade_setting (id, backup_enabled, backup_timeout_seconds, health_timeout_seconds, note)
values (1, true, 600, 90, '');
