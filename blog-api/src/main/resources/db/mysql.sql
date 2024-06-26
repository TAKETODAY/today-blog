create database if not exists `today`;

use `today`;

create table attachment
(
    `id`        bigint auto_increment primary key,
    `name`      varchar(255) default null comment '附件名称',
    `uri`       mediumtext   default null comment 'URI地址',
    `location`  mediumtext   default null comment '附件本地地址',
    `size`      bigint       default 0 comment '文件大小',
    `sync`      bit          default b'0' comment '',
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
    `id`        int unsigned auto_increment primary key,
    `title`     varchar(255) default not null comment '标题',
    `content`   longtext     default not null comment '文章内容',
    `summary`   text         default not null comment '摘要',
    `cover`     text         default null comment '文章封面',
    `category`  varchar(255) default '未分类' comment '分类名',
    `uri`       varchar(255) default not null comment '文章URI访问地址',
    `pv`        int          default 0 comment '点击量',
    `status`    tinyint      default 0 comment '状态',
    `markdown`  longtext     default null comment 'markdown',
    `password`  varchar(255) default default           null null comment '密码',
    `copyright` text         default null comment '版权',

    `create_at` datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    `update_at` datetime     default CURRENT_TIMESTAMP not null comment '更新时间',

    constraint uri_idx unique (uri),
    constraint Article_Category
        foreign key (category) references category (name)
            on update cascade on delete set null
);

create table label
(
    `id`   int auto_increment comment '主键' primary key,
    `name` varchar(255) not null comment '标签名'
);

create table article_label
(
    `labelId`   int not null comment '主键',
    `articleId` int not null comment '文章id',
    primary key (labelId, articleId)
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

create table `option`
(
    `name`  varchar(255) not null primary key,
    `value` text         null
);

# @formatter:off
INSERT INTO `option` (name, value) VALUES ('article.feed.list.size', '10');
INSERT INTO `option` (name, value) VALUES ('comment.check', 'true');
INSERT INTO `option` (name, value) VALUES ('comment.content.length', '10240');
INSERT INTO `option` (name, value) VALUES ('comment.list.size', '10');
INSERT INTO `option` (name, value) VALUES ('comment.placeholder', '赶快评论一个吧！');
INSERT INTO `option` (name, value) VALUES ('comment.send.mail', 'false');
INSERT INTO `option` (name, value) VALUES ('mail.enable', 'true');
INSERT INTO `option` (name, value) VALUES ('site.author.email', 'taketoday@foxmail.com');
INSERT INTO `option` (name, value) VALUES ('site.cdn', 'https://cdn.taketoday.cn');
INSERT INTO `option` (name, value) VALUES ('site.copyright', 'Copyright © TODAY & 2017 - 2023 All Rights Reserved.');
INSERT INTO `option` (name, value) VALUES ('site.description', '');
INSERT INTO `option` (name, value) VALUES ('site.host', 'https://taketoday.cn');
INSERT INTO `option` (name, value) VALUES ('site.icp', '');
INSERT INTO `option` (name, value) VALUES ('site.keywords', '电子，编程，Java，分享，STM32，51单片机，ARM，杨海健，开源项目.');
INSERT INTO `option` (name, value) VALUES ('site.list.size', '8');
INSERT INTO `option` (name, value) VALUES ('site.max.list.size', '100');
INSERT INTO `option` (name, value) VALUES ('site.name', 'TODAY BLOG');
INSERT INTO `option` (name, value) VALUES ('site.otherFooter', '');
INSERT INTO `option` (name, value) VALUES ('site.subTitle', '代码是我心中的一首诗');
INSERT INTO `option` (name, value) VALUES ('site.upload', '/data/docs');
INSERT INTO `option` (name, value) VALUES ('site.version', 'v3.1');

-- @formatter:on

create table page_view
(
    `id`             bigint unsigned auto_increment
        primary key,
    `url`            tinytext                           not null,
    `user`           varchar(255)                       null,
    `referer`        varchar(1024)                      null,
    `ip`             varchar(255)                       null,
    `user_agent`     text                               null,
    `browser`        varchar(255)                       null,
    `device`         varchar(255)                       null,
    `os`             varchar(255)                       null,
    `browserVersion` varchar(32)                        null,
    `create_at`      datetime default CURRENT_TIMESTAMP null
)
    engine = MyISAM
    collate = utf8mb4_bin;

create table user
(
    `id`           bigint                                                  not null comment '注册时间戳',
    `name`         varchar(255) default '无名氏'                           null comment '姓名，昵称',
    `email`        varchar(64)                                             not null comment '邮箱',
    `introduce`    varchar(255) default '暂无'                             null comment '描述',
    `site`         varchar(255)                                            null comment '个人网站',
    `password`     varchar(64)  default '8E33AE4E09847AB123435A358CD5CF2D' not null comment '密码默认密码：https://taketoday.cn MD5：8E33AE4E09847AB123435A358CD5CF2D',
    `type`         varchar(255)                                            null comment '类型:qq,github,site,master',
    `avatar`       text                                                    null comment '头像',
    `background`   varchar(255) default '/assets/images/bg/info_back.jpg'  null comment '背景',
    `status`       int          default 0                                  not null comment '状态:(0:正常,1:未激活,2:账号被锁,3:账号删除)',
    `notification` bit          default b'1'                               null,
    primary key (id, email)
);

create table t_comment
(
    `id`         bigint             not null auto_increment primary key,
    `article_id` bigint             null comment '评论文章id',
    `user_id`    bigint             null comment '评论者id',
    `content`    text               not null comment '评论内容',
    `status`     tinyint  default 0 not null comment '状态（审核，未审核，回收站）',
    `comment_id` bigint             null comment '父评论id',

    `create_at`  datetime default CURRENT_TIMESTAMP comment '创建时间',
    `update_at`  datetime on update CURRENT_TIMESTAMP comment '更新时间'
);
