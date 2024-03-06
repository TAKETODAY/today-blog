use today;

# @formatter:off

# @formatter:on

alter table category
    change articleCount article_count int default 0 null comment '文章数';


SELECT *
FROM category
order by `order` asc