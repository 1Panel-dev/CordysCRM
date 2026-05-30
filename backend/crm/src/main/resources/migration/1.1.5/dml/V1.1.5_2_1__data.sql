-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

SET @customer_concat_addID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_concat_addID, 'CUSTOMER_CONCAT_ADD', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_collaboration_addID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_collaboration_addID, 'CUSTOMER_COLLABORATION_ADD', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_convert_businessID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_convert_businessID, 'CLUE_CONVERT_BUSINESS', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );


SET SESSION innodb_lock_wait_timeout = DEFAULT;