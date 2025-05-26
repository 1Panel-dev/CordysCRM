-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化用户
INSERT INTO `sys_user` ( `id`, `name`, `email`, `password`, `gender`, `phone`, `language`, `last_organization_id`, `create_time`, `update_time`, `create_user`, `update_user` )
VALUES
    ( 'admin', 'Administrator', 'admin@cordys-crm.io', MD5( 'CordysCRM' ), 1, '4000520755', 'zh_CN', '100001', 1716175907000, 1729752373360, '717345437786112', 'admin' );

SET @internal_department_id = UUID_SHORT();-- 初始化用户基本信息

INSERT INTO `sys_department` ( `id`, `name`, `organization_id`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`, `resource`, `resource_id` )
VALUES
    ( @internal_department_id, '公司名称', '100001', 'NONE', 100001, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL );-- 初始化默认组织

INSERT INTO `sys_organization` ( `id`, `name`, `create_time`, `update_time`, `create_user`, `update_user` )
VALUES
    ( '100001', 'default', 1736152274610, 1736152274610, 'admin', 'admin' );-- 初始化组织管理员

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'org_admin', 'org_admin', 1, 'ALL', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin', '', '100001' );-- 初始化销售经理

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'sales_manager', 'sales_manager', 1, 'DEPT_AND_CHILD', UNIX_TIMESTAMP() * 1000 + 1, UNIX_TIMESTAMP() * 1000 + 1, 'admin', 'admin', '', '100001' );-- 初始化销售专员

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'sales_staff', 'sales_staff', 1, 'SELF', UNIX_TIMESTAMP() * 1000 + 2, UNIX_TIMESTAMP() * 1000 + 2, 'admin', 'admin', '', '100001' );-- set innodb lock wait timeout to default

SET @customer_addID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_addID, 'CUSTOMER_ADD', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_transferred_customerID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_transferred_customerID, 'CUSTOMER_TRANSFERRED_CUSTOMER', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_automatic_move_high_seasID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_automatic_move_high_seasID, 'CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_moved_high_seasID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_moved_high_seasID, 'CUSTOMER_MOVED_HIGH_SEAS', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );


SET @customer_deletedID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_deletedID, 'CUSTOMER_DELETED', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @high_seas_customer_distributedID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@high_seas_customer_distributedID, 'HIGH_SEAS_CUSTOMER_DISTRIBUTED', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @customer_follow_up_plan_dueID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@customer_follow_up_plan_dueID, 'CUSTOMER_FOLLOW_UP_PLAN_DUE', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_automatic_move_poolID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_automatic_move_poolID, 'CLUE_AUTOMATIC_MOVE_POOL', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_moved_poolID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_moved_poolID, 'CLUE_MOVED_POOL', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_convert_customerID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_convert_customerID, 'CLUE_CONVERT_CUSTOMER', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @transfer_clueID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@transfer_clueID, 'TRANSFER_CLUE', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_deletedID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_deletedID, 'CLUE_DELETED', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_distributedID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_distributedID, 'CLUE_DISTRIBUTED', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_importID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_importID, 'CLUE_IMPORT', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @clue_follow_up_plan_dueID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@clue_follow_up_plan_dueID, 'CLUE_FOLLOW_UP_PLAN_DUE', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @business_deletedID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@business_deletedID, 'BUSINESS_DELETED', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @business_transferID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@business_transferID, 'BUSINESS_TRANSFER', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @business_importID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@business_importID, 'BUSINESS_IMPORT', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET @business_follow_up_plan_dueID = UUID_SHORT();-- 初始化消息通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@business_follow_up_plan_dueID, 'BUSINESS_FOLLOW_UP_PLAN_DUE', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET SESSION innodb_lock_wait_timeout = DEFAULT;