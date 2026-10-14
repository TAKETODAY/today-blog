create table t_api_key
(
    id           bigint unsigned auto_increment primary key,
    user_id      bigint unsigned not null,
    name         varchar(100) not null,
    key_hash     char(64) not null,
    created_at   datetime not null default CURRENT_TIMESTAMP,
    last_used_at datetime null,
    revoked_at   datetime null,
    constraint t_api_key_hash_idx unique (key_hash),
    constraint api_key_user_fk foreign key (user_id) references user (id)
        on delete cascade
);
