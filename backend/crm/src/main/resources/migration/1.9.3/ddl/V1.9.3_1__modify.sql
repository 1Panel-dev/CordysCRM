-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE schedule ADD COLUMN update_user varchar(32) NOT NULL COMMENT '更新人';
ALTER TABLE opportunity_stage_config ADD COLUMN circulation_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '流转类型(NORMAL-普通，ADVANCED-高级)';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
