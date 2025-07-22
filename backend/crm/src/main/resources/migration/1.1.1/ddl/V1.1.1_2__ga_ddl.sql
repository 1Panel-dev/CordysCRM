-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Add pool pick rule
ALTER TABLE customer_pool_pick_rule
    ADD limit_new BIT(1) NOT NULL DEFAULT 0 COMMENT '是否限制新数据' after pick_interval_days;
ALTER TABLE customer_pool_pick_rule
    ADD new_pick_interval INT COMMENT '新数据领取保护' after limit_new;

ALTER TABLE clue_pool_pick_rule
    ADD limit_new BIT(1) NOT NULL DEFAULT 0 COMMENT '是否限制新数据' after pick_interval_days;
ALTER TABLE clue_pool_pick_rule
    ADD new_pick_interval INT COMMENT '新数据领取保护' after limit_new;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



