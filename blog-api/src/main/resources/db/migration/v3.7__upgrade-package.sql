create table t_upgrade_package
(
    id            char(36)      not null primary key,
    original_name varchar(255)  not null,
    status        varchar(32)   not null,
    version       varchar(100)  not null,
    build         bigint        not null,
    file_size     bigint        not null,
    sha256        char(64)      not null,
    entry_count   int           not null default 0,
    operator_id   bigint        null,
    created_at    varchar(40)   not null,
    consumed_at   varchar(40)   null,
    operation_id  char(36)      null,
    message       varchar(1000) null,
    metadata      longtext      null,
    index idx_upgrade_package_created (created_at),
    index idx_upgrade_package_build (build),
    index idx_upgrade_package_operation (operation_id),
    index idx_upgrade_package_sha256 (sha256)
);