use today;

# @formatter:off

# @formatter:on

alter table t_comment
    add email          varchar(255) not null comment 'email',
    add commenter      varchar(255) not null comment '评论者的名字',
    add commenter_site varchar(255) default null comment '评论者的网站地址',
    add article_title  varchar(255) default null comment '文章标题',
    change comment_id parent_id bigint unsigned default null comment '父评论id'
;
