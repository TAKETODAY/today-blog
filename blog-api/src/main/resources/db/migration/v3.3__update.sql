alter table t_option
    add update_at datetime on update CURRENT_TIMESTAMP comment '更新时间';

alter table t_option
    add value_type varchar(255) default 'string' not null comment '值类型' after value;
