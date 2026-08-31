-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- API 请求地址列放宽到 255
ALTER TABLE agent_model MODIFY COLUMN `api_url` VARCHAR(255) COMMENT 'API请求地址';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
