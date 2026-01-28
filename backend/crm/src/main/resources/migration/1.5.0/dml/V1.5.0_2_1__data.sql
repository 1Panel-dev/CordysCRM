-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化消息通知
SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'BUSINESS_QUOTATION_EXPIRING', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'BUSINESS_QUOTATION_EXPIRING', 'OPPORTUNITY','{"timeList":[{"timeValue":3,"timeUnit":"DAY"}],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');


SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'BUSINESS_QUOTATION_EXPIRED', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );


SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'BUSINESS_QUOTATION_EXPIRED', 'OPPORTUNITY','{"timeList":[],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_ARCHIVED', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_ARCHIVED', 'CONTRACT','{"timeList":[],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_VOID', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_VOID', 'CONTRACT','{"timeList":[],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_EXPIRING', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_EXPIRING', 'CONTRACT','{"timeList":[{"timeValue":3,"timeUnit":"DAY"}],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_EXPIRED', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_EXPIRED', 'CONTRACT','{"timeList":[],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');


SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_PAYMENT_EXPIRING', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_PAYMENT_EXPIRING', 'CONTRACT','{"timeList":[{"timeValue":3,"timeUnit":"DAY"}],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CONTRACT_PAYMENT_EXPIRED', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_addID = UUID_SHORT();
INSERT INTO sys_message_task_config (id, organization_id, event, task_type,  value)
    VALUE (@customer_addID, '100001', 'CONTRACT_PAYMENT_EXPIRED', 'CONTRACT','{"timeList":[],"userIds":["OWNER"],"roleIds":[],"ownerEnable":false,"ownerLevel":0,"roleEnable":false}');


SET SESSION innodb_lock_wait_timeout = DEFAULT;