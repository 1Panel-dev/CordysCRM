-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 历史模型配置默认归属系统层级
UPDATE agent_model SET `scope` = 'SYSTEM';

-- 自定义表单审批结果通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (UUID_SHORT(), 'CUSTOM_FORM_DATA_APPROVAL', 'APPROVAL', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET SESSION innodb_lock_wait_timeout = DEFAULT;
