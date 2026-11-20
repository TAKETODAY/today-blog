insert into t_option (name, value, `public`, description, value_type)
values ('site.locale', 'zh-CN', true, '站点默认语言（zh-CN 或 en-US）', 'string')
on duplicate key update name = name;

insert into t_option (name, value, `public`, description, value_type)
values ('site.timezone', 'Asia/Shanghai', true, '站点默认时区（IANA 标识）', 'string')
on duplicate key update name = name;
