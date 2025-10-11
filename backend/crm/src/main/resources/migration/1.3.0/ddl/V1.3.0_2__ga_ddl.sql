-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table sys_message_task
    add ding_talk_enable bit default b'0' null comment '钉钉启用' after we_com_enable;

alter table sys_message_task
    add lark_enable bit default b'0' null comment '飞书启用' after ding_talk_enable;


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



