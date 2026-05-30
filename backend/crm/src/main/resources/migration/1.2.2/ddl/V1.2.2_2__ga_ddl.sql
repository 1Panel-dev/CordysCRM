-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE opportunity ADD COLUMN pos BIGINT DEFAULT NULL;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



