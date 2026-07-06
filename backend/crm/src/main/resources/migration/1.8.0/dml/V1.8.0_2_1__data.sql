-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (UUID_SHORT(), 'APPROVAL_CC', 'APPROVAL', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

SET SESSION innodb_lock_wait_timeout = DEFAULT;