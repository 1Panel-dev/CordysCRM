-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table opportunity_quotation
    add until_time bigint not null comment '有效期至';


SET SESSION innodb_lock_wait_timeout = DEFAULT;