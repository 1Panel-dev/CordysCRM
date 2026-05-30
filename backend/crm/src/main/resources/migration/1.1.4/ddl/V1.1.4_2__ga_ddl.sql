-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Add follow_up_plan transit_record column
alter table follow_up_plan
    add converted bit default b'0' not null comment '是否转为跟进记录' after status;


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;