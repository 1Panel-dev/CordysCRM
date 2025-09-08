-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Allow opportunity customer_id null
ALTER TABLE opportunity MODIFY customer_id  VARCHAR(32) NULL COMMENT '客户ID';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;