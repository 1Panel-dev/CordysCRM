-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- API 请求地址列放宽到 255
ALTER TABLE agent_model MODIFY COLUMN `api_url` VARCHAR(255) COMMENT 'API请求地址';

ALTER TABLE custom_form_data ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '审批状态';

ALTER TABLE custom_form_data ADD COLUMN approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
