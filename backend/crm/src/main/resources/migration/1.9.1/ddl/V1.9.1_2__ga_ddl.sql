-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- API URL 长度放宽到 255字符
ALTER TABLE agent_model MODIFY COLUMN `api_url` VARCHAR(255) COMMENT 'API请求地址';

-- 历史消息支持状态
ALTER TABLE agent_message
    ADD COLUMN `status` VARCHAR(20) NULL COMMENT '消息状态：done 已完成，stopped 已停止' AFTER `helpful`;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
