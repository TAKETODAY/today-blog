alter table t_mail
    change column `to` recipient varchar(255) not null comment '收件人',
    modify content mediumtext not null comment '邮件内容',
    modify sent_at datetime null comment '发送成功时间',
    add status varchar(16) not null default 'SENT' comment 'PENDING/SENDING/SENT/FAILED',
    add attempts int not null default 0 comment '发送尝试次数',
    add last_attempt_at datetime null comment '最近发送尝试时间',
    add last_error text null comment '最近失败原因',
    add attachment varchar(1024) null comment '附件路径',
    add index idx_mail_status_id (status, id);

alter table t_mail alter column status set default 'PENDING';
