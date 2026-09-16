-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE customer
    ADD COLUMN frozen TINYINT(1) DEFAULT 0 NULL COMMENT '是否冻结',
    ADD COLUMN freeze_reason VARCHAR(300) NULL COMMENT '冻结原因',
    ADD COLUMN unfreeze_time BIGINT NULL COMMENT '自动解冻时间，永久冻结时为空';

CREATE INDEX idx_customer_freeze_expire ON customer (frozen, unfreeze_time);

ALTER TABLE clue
    ADD COLUMN frozen TINYINT(1) DEFAULT 0 NULL COMMENT '是否冻结',
    ADD COLUMN freeze_reason VARCHAR(300) NULL COMMENT '冻结原因',
    ADD COLUMN unfreeze_time BIGINT NULL COMMENT '自动解冻时间，永久冻结时为空';

CREATE INDEX idx_clue_freeze_expire ON clue (frozen, unfreeze_time);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
