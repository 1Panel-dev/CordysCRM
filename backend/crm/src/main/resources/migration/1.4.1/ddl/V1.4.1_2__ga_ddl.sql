-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify lead phone length (255)
ALTER TABLE clue MODIFY COLUMN phone VARCHAR(255) NULL COMMENT '联系人电话';

ALTER TABLE contract DROP COLUMN archived_status;

ALTER TABLE contract RENAME COLUMN status TO stage;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;