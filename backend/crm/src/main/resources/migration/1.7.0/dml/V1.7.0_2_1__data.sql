-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO `contract_stage_config`(`id`, `name`, `type`, `afoot_roll_back`, `end_roll_back`, `pos`, `organization_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('PENDING_SIGNING', '待签署', 'AFOOT', b'1', b'0', 1, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('SIGNED', '已签署', 'AFOOT', b'1', b'0', 2, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('CHANGE', '合同变更', 'AFOOT', b'1', b'0', 3, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('IN_PROGRESS', '履行中', 'AFOOT', b'1', b'0', 4, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('COMPLETED_PERFORMANCE', '履行完毕', 'AFOOT', b'1', b'0', 5, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('ARCHIVED', '合同完结', 'END', b'1', b'0', 6, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('VOID', '作废', 'END', b'1', b'0', 7, '100001', 1776417799080, 1776417799080, 'admin', 'admin');


-- 初始化消息通知
SET @contract_approval_id = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@contract_approval_id, 'CONTRACT_APPROVAL', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );
SET @order_approval_id = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@order_approval_id, 'ORDER_APPROVAL', 'ORDER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );
SET @invoice_approval_id = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@invoice_approval_id, 'INVOICE_APPROVAL', 'CONTRACT', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );
SET @approval_todo_id = UUID_SHORT();
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (@approval_todo_id, 'APPROVAL_TODO', 'APPROVAL', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET SESSION innodb_lock_wait_timeout = DEFAULT;