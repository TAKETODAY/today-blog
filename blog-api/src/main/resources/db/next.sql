use today;

# @formatter:off

# @formatter:on

alter table category
    change articleCount article_count int default 0 null comment '文章数';


alter table `option`
    rename t_option
;
