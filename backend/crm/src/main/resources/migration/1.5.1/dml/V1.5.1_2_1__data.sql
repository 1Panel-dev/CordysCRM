-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化消息通知
SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'SYNC_ORGANIZATION_STRUCTURE', 'SYSTEM', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

-- init advance search settings
INSERT INTO sys_parameter value ('advance.search.setting', 'false', 'text');

SET SESSION innodb_lock_wait_timeout = DEFAULT;