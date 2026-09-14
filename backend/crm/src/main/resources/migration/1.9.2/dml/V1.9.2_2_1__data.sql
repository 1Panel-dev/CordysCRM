-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 历史模型配置默认归属系统层级
UPDATE agent_model SET `scope` = 'SYSTEM';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
