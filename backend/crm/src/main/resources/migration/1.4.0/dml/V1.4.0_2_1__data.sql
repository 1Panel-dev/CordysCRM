-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

insert into sys_module value (UUID_SHORT(), '100001', 'contract', true, 9,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_module value (UUID_SHORT(), '100001', 'tender', true, 10,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

SET @customer_addID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'BUSINESS_QUOTATION_APPROVAL', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'BUSINESS_QUOTATION_DELETED', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET SESSION innodb_lock_wait_timeout = DEFAULT;