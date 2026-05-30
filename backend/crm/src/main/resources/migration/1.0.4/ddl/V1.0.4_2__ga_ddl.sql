-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 改成非必填
ALTER TABLE opportunity MODIFY COLUMN expected_end_time bigint(20) NULL COMMENT '结束时间';
ALTER TABLE opportunity MODIFY COLUMN products varchar(1000) NULL COMMENT '意向产品';
ALTER TABLE clue MODIFY COLUMN products varchar(1000) NULL COMMENT '意向产品';
ALTER TABLE product MODIFY COLUMN price DECIMAL(14, 4) NULL COMMENT '价格';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



