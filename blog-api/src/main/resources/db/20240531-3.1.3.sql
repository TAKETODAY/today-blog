use today;

# @formatter:off

# @formatter:on

alter table category
    change articleCount article_count int default 0 null comment '文章数';


alter table `option`
    rename t_option
;

create table t_page_view
(
    `id`              bigint unsigned auto_increment primary key,

    `url`             text                               not null,
    `user`            varchar(255)                       null comment '登陆用户email',

    `referer`         varchar(1024),
    `user_agent`      text,

    `os`              varchar(255)                       null,
    `device`          varchar(255)                       null,

    `browser`         varchar(255)                       null,
    `browser_version` varchar(32)                        null,

    `ip`              varchar(255)                       null,
    `ip_country`      varchar(64)                        null comment 'IP国家',
    `ip_province`     varchar(64)                        null comment 'IP省份',
    `ip_city`         varchar(64)                        null comment 'IP城市',
    `ip_area`         varchar(64)                        null comment 'IP区县',
    `ip_isp`          varchar(64)                        null comment 'ISP',

    `create_at`       datetime default CURRENT_TIMESTAMP not null
) comment 'page view stat';

alter table article
    modify create_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    modify update_at datetime default CURRENT_TIMESTAMP not null comment '更新时间',
    modify uri varchar(255) not null comment '文章URI访问地址';
