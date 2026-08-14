-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init agent term catalog
INSERT INTO agent_term_catalog
VALUES
(UUID_SHORT(), '销售类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '客户类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '合同财务类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '流程审批类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

INSERT INTO sys_message_task
(id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES
    (UUID_SHORT(), 'CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED', 'CUSTOMER', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED', 'CUSTOMER', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CUSTOMER_FOLLOW_UP_RECORD_COMMENT_ADDED', 'CUSTOMER', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CUSTOMER_FOLLOW_UP_RECORD_COMMENT_MENTIONED', 'CUSTOMER', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),

    (UUID_SHORT(), 'CLUE_FOLLOW_UP_PLAN_COMMENT_ADDED', 'CLUE', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CLUE_FOLLOW_UP_PLAN_COMMENT_MENTIONED', 'CLUE', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CLUE_FOLLOW_UP_RECORD_COMMENT_ADDED', 'CLUE', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'CLUE_FOLLOW_UP_RECORD_COMMENT_MENTIONED', 'CLUE', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),

    (UUID_SHORT(), 'OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_ADDED', 'OPPORTUNITY', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_MENTIONED', 'OPPORTUNITY', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_ADDED', 'OPPORTUNITY', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_MENTIONED', 'OPPORTUNITY', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2);


SET SESSION innodb_lock_wait_timeout = DEFAULT;