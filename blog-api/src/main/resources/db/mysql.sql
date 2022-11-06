create schema `today-blog`;

use `today-blog`;

create table attachment
(
    `id`        bigint                    not null primary key,
    `name`      varchar(255) charset utf8 null,
    `url`       varchar(255) charset utf8 null,
    `location`  text charset utf8         null,
    `sync`      bit    default b'0'       null,
    `size`      bigint default 0          null,
    `file_type` varchar(255) charset utf8 null
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
    articleCount  int           default 0              null comment '文章数',
    `order`       int unsigned  default '128'          null comment '分类排序',
    `description` varchar(1024) default '未分类的文章' not null comment '分类描述'
);

create table article
(
    `id`         bigint                                                                                         not null comment '创建时间为主键'
        primary key,
    `title`      varchar(255)                                                                                   null comment '题目',
    `content`    longtext                                                                                       null comment '文章内容',
    `copyRight`  varchar(255) default '版权声明：本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。' null comment '版权',
    `image`      varchar(255)                                                                                   null comment '题图',
    `lastModify` bigint                                                                                         null comment '最后更改',
    `summary`    text                                                                                           null comment '预览',
    `category`   varchar(255) default '未分类'                                                                  null comment '分类名',
    `pv`         int          default 0                                                                         null comment '点击量',
    `status`     int          default 0                                                                         not null comment '状态',
    `url`        varchar(255)                                                                                   null comment '文章url',
    `markdown`   longtext                                                                                       null comment 'markdown',
    `password`   varchar(255)                                                                                   null,
    constraint Article_Category
        foreign key (`category`) references category (name)
            on update cascade on delete set null
);

create table label
(
    `id`   bigint auto_increment comment '主键' primary key,
    `name` varchar(255) not null comment '标签名'
);

create table article_label
(
    `labelId`   bigint not null comment '主键',
    `articleId` bigint not null comment '文章id',
    primary key (labelId, articleId)
);

create table logger
(
    `id`      bigint        not null comment '时间戳' primary key,
    `title`   varchar(255)  not null comment '日志标题',
    `content` varchar(1024) not null comment '日志内容',
    `ip`      varchar(64)   not null comment '操作者ip归属地',
    `user`    varchar(32)   null comment '操作者email',
    `type`    varchar(16)   null comment '类型',
    `result`  varchar(255)  null comment '成功，失败，警告'
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
INSERT INTO `option` (name, value) VALUES ('site.copyright', 'Copyright © TODAY & 2017 - 2021 All Rights Reserved.');
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
INSERT INTO `option` (name, value) VALUES ('site.version', 'TODAY BLOG v3.1');
# @formatter:on

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

create table comment
(
    `id`         bigint                  not null comment '评论时间戳' primary key,
    `articleId`  bigint                  null comment '评论文章id',
    `replyUser`  bigint                  null comment '回复者id',
    `content`    text                    not null comment '评论内容',
    `status`     tinyint(1) default 0    not null comment '状态（审核，未审核，回收站）',
    `commentId`  bigint                  null comment '父评论id',
    `lastModify` bigint     default 0    null comment '最后一次更改',
    `sendMail`   bit        default b'0' null comment '是否已经发送过邮件'
);
