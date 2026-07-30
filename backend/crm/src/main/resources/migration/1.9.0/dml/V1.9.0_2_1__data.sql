-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init agent term catalog
INSERT INTO agent_term_catalog
VALUES
(UUID_SHORT(), '销售类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '客户类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '合同财务类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
(UUID_SHORT(), '流程审批类', '100001', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;