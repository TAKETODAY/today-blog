use today;

# @formatter:off

# @formatter:on

alter table article_label
    change labelId label_id bigint not null comment 'tag主键',
    change articleId article_id bigint not null comment '文章id';
