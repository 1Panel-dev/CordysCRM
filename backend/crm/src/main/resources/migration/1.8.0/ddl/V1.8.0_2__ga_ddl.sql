-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE clue_pool_hidden_field MODIFY COLUMN field_id varchar(255) NOT NULL COMMENT '字段ID';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
