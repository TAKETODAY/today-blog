create table t_frontend_cache
(
    id       tinyint not null primary key,
    policies text    not null
);

insert into t_frontend_cache (id, policies) values (1, '{}');
