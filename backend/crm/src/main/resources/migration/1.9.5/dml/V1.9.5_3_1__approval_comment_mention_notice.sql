SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_message_task
    (id, event, task_type, email_enable, sys_enable, organization_id, template,
     create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), 'APPROVAL_COMMENT_MENTIONED', 'APPROVAL', FALSE, TRUE, org.id, NULL,
       'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_organization org
WHERE NOT EXISTS (
    SELECT 1 FROM sys_message_task task
    WHERE task.organization_id = org.id
      AND task.task_type = 'APPROVAL'
      AND task.event = 'APPROVAL_COMMENT_MENTIONED'
);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
