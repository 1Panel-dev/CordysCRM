-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table product
    add pos BIGINT NOT NULL DEFAULT 0 COMMENT '排序' after status;




-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



