create database if not exists `today`;

use `today`;

create table attachment
(
    `id`        bigint auto_increment primary key,
    `name`      varchar(255) default null comment '附件名称',
    `uri`       mediumtext   default null comment 'URI地址',
    `location`  mediumtext   default null comment '附件本地地址',
    `size`      bigint       default 0 comment '文件大小',
    `sync`      bit          default false comment '同步状态',
    `file_type` varchar(255) default null comment '附件类型',

    `create_at` datetime     default CURRENT_TIMESTAMP comment '创建时间',
    `update_at` datetime on update CURRENT_TIMESTAMP comment '更新时间'
);

create table blogger
(
    `id`        int auto_increment comment '主键' primary key,
    `name`      varchar(255)                                      null comment '博主名字',
    `age`       int                                               null comment '年龄',
    `email`     varchar(255)                                      null comment '邮件',
    `introduce` varchar(255)                                      null comment '自我介绍',
    `passwd`    varchar(255)                                      null comment '密码',
    `image`     varchar(255) default '/upload/avatar/default.png' null,
    `sex`       varchar(12)                                       null comment '性别',
    `address`   varchar(255)                                      null comment '地址'
);

create table category
(
    `name`        varchar(255)  default '未分类'       not null comment '类型名称'
        primary key,
    article_count int           default 0              null comment '文章数',
    `order`       int unsigned  default '128'          null comment '分类排序',
    `description` varchar(1024) default '未分类的文章' not null comment '分类描述'
);

create table article
(
    `id`        bigint unsigned auto_increment primary key,
    `title`     varchar(255)                           not null comment '标题',
    `content`   longtext     default null comment '文章内容',
    `summary`   text         default null comment '摘要',
    `cover`     text                                   null comment '文章封面',
    `category`  varchar(255) default '未分类' comment '分类名',
    `uri`       varchar(255)                           not null comment '文章URI访问地址',
    `pv`        int          default 0 comment '点击量',
    `status`    tinyint      default 0 comment '状态',
    `markdown`  longtext     default null comment 'markdown',
    `password`  varchar(255) default null              null comment '密码',
    `copyright` text         default null comment '版权',

    `create_at` datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    `update_at` datetime     default CURRENT_TIMESTAMP not null comment '更新时间',
    pinned_at   datetime(6)                            null comment '首页置顶时间，NULL 表示未置顶',

    constraint uri_idx unique (uri),
    fulltext index idx_article_content (content),
    index idx_article_home (status, pinned_at, create_at, id)
);

create table label
(
    `id`   int auto_increment comment '主键' primary key,
    `name` varchar(255) not null comment '标签名'
);

create table article_label
(
    label_id   int not null comment '主键',
    article_id int not null comment '文章id',
    primary key (label_id, article_id)
);

create table logging
(
    `id`          bigint auto_increment primary key,
    `title`       varchar(255) not null comment '日志标题',
    `content`     text         not null comment '日志内容',
    `ip`          varchar(64)  not null comment '操作者IP',

    `ip_country`  varchar(64)  null comment '操作者IP国家',
    `ip_province` varchar(64)  null comment '操作者IP省份',
    `ip_city`     varchar(64)  null comment '操作者IP城市',
    `ip_area`     varchar(64)  null comment '操作者IP区县',
    `ip_isp`      varchar(64)  null comment '操作者ISP',

    `user`        varchar(32)  null comment '操作者email',
    `type`        varchar(16)  null comment '类型',
    `invoke_at`   datetime     not null comment '调用时间',
    `create_at`   datetime default CURRENT_TIMESTAMP comment '创建时间'

);

create table t_option
(
    name        varchar(255)                  not null primary key,
    value       text         default null,
    value_type  varchar(255) default 'string' not null comment '值类型',
    description text         default null comment 'description',
    `public`    bit          default true     not null comment 'public',
    update_at   datetime on update CURRENT_TIMESTAMP comment '更新时间'
);

create table t_page_view
(
    id              bigint unsigned auto_increment primary key,

    url             text                                   not null,
    host            varchar(255)                           not null comment 'request host',
    path            varchar(255)                           not null comment 'request path',

    `user`          varchar(255) default null comment '登陆用户 email',

    referer         varchar(1024),
    user_agent      text         default null,

    os              varchar(255) default null,
    device          varchar(255) default null,

    browser         varchar(255) default null,
    browser_version varchar(32)  default null,

    ip              varchar(255) default null,
    ip_country      varchar(64)  default '*' comment 'IP国家',
    ip_province     varchar(64)  default '*' comment 'IP省份',
    ip_city         varchar(64)  default '*' comment 'IP城市',
    ip_area         varchar(64)  default '*' comment 'IP区县',
    ip_isp          varchar(64)  default '*' comment 'ISP',

    create_at       datetime     default CURRENT_TIMESTAMP not null
) comment 'page view stat';

create table user
(
    `id`           bigint unsigned auto_increment primary key,
    `name`         varchar(255) default '无名氏'                                                       null comment '姓名，昵称',
    `email`        varchar(64)                                                                         not null comment '邮箱',
    `introduce`    varchar(255) default '暂无'                                                         null comment '描述',
    `site`         varchar(255)                                                                        null comment '个人网站',
    `password`     varchar(255) default '$2a$10$2FOYgUohsSb6oQeLo7s6vOAl6set5A2REF68u5xfhiw8J7cuxXmLa' not null,
    `type`         varchar(255)                                                                        null comment '类型:qq,github,site,master',
    `avatar`       text                                                                                null comment '头像',
    `background`   varchar(255) default '/assets/images/bg/info_back.jpg'                              null comment '背景',
    `status`       int          default 0                                                              not null comment '状态:(0:正常,1:未激活,2:账号被锁,3:账号删除)',
    `notification` bit          default b'1'                                                           null,

    constraint email_idx
        unique (email)
);

create table t_comment
(
    `id`             bigint unsigned            not null auto_increment primary key,
    `content`        text                       not null comment '评论内容',
    `email`          varchar(255)               not null comment 'email',
    `commenter`      varchar(255)               not null comment '评论者的名字',
    `commenter_site` varchar(255)     default null comment '评论者的网站地址',
    `article_title`  varchar(255)     default null comment '文章标题',
    `status`         tinyint unsigned default 0 not null comment '状态（审核，未审核，回收站）',
    `article_id`     bigint unsigned            not null comment '评论文章id',

    `parent_id`      bigint unsigned  default null comment '父评论id',
    `user_id`        bigint unsigned  default null comment '评论者id',

    `create_at`      datetime         default CURRENT_TIMESTAMP comment '创建时间',
    `update_at`      datetime on update CURRENT_TIMESTAMP comment '更新时间'
);

create table t_mail
(
    id              bigint unsigned not null auto_increment primary key,

    content         mediumtext      not null comment '邮件内容',

    recipient       varchar(255)    not null comment '收件人',
    subject         varchar(255)    not null comment '主题',


    sent_at         datetime        null comment '发送成功时间',

    create_at       datetime                 default CURRENT_TIMESTAMP comment '创建时间',
    update_at       datetime on update CURRENT_TIMESTAMP comment '更新时间',
    status          varchar(16)     not null default 'PENDING' comment 'PENDING/SENDING/SENT/FAILED',
    attempts        int             not null default 0 comment '发送尝试次数',
    last_attempt_at datetime        null comment '最近发送尝试时间',
    last_error      text            null comment '最近失败原因',
    attachment      varchar(1024)   null comment '附件路径',
    index idx_mail_status_id (status, id)
);

create table t_api_key
(
    id           bigint unsigned auto_increment primary key,
    user_id      bigint unsigned not null,
    name         varchar(100)    not null,
    key_hash     char(64)        not null,
    created_at   datetime        not null default CURRENT_TIMESTAMP,
    last_used_at datetime        null,
    revoked_at   datetime        null,
    constraint t_api_key_hash_idx unique (key_hash),
    constraint api_key_user_fk foreign key (user_id) references user (id)
        on delete cascade
);

create table t_backup
(
    id                  bigint unsigned not null auto_increment primary key,
    name                varchar(200)  not null,
    status              varchar(32)   not null,
    version             varchar(100)  null,
    build               varchar(32)   null,
    created_at          datetime      not null,
    protected_backup    boolean       not null default false,
    manifest_sha256     char(64)      null,
    total_bytes         bigint        not null default 0,
    source_operation_id bigint unsigned null,
    location            varchar(1024) null,
    scope               varchar(500)  null,
    restore_verified_at datetime      null
);

create table t_release_mapping
(
    id         bigint unsigned not null auto_increment primary key,
    name       varchar(32)  not null,
    mode       varchar(16)  not null,
    source     varchar(255) not null,
    target     varchar(512) not null,
    created_at datetime     not null,
    updated_at datetime     not null
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
    created_at    datetime      not null,
    consumed_at   datetime      null
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
    backup_completed_at          datetime      null,
    message                      varchar(1000) null,
    created_at                   datetime      not null,
    started_at                   datetime      null,
    finished_at                  datetime      null
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
