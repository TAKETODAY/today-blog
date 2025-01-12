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

alter table t_page_view
    add host varchar(255) not null comment 'request host' after url,
    add path varchar(255) not null comment 'request path' after host;


alter table t_option
    add public      bit  not null default true comment 'public',
    add description text null comment 'description';

# @formatter:off
update t_page_view set ip_area = '*' where ip_area = 'UNKNOWN';
update t_page_view set ip_city = '*' where ip_city = 'UNKNOWN';
update t_page_view set ip_isp = '*' where ip_isp = 'UNKNOWN';
update t_page_view set ip_province = '*' where ip_province = 'UNKNOWN';
update t_page_view set ip_country = '*' where ip_country = 'UNKNOWN';

update logging set ip_area = '*' where ip_area = 'UNKNOWN';
update logging set ip_city = '*' where ip_city = 'UNKNOWN';
update logging set ip_isp = '*' where ip_isp = 'UNKNOWN';
update logging set ip_province = '*' where ip_province = 'UNKNOWN';
update logging set ip_country = '*' where ip_country = 'UNKNOWN';
