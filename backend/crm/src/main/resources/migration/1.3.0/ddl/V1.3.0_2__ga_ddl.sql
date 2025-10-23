-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE dashboard CHANGE COLUMN resource_id resource_url VARCHAR(100) NOT NULL COMMENT '仪表板url';


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



