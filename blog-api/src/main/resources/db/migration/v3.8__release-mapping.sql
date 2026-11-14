create table t_release_mapping
(
    name        varchar(32)  not null primary key,
    mode        varchar(16)  not null,
    source      varchar(255) not null,
    target      varchar(512) not null,
    operator_id bigint       null,
    created_at  varchar(40)  not null,
    updated_at  varchar(40)  not null
);
