-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- init dict config
INSERT INTO sys_dict values (UUID_SHORT(), '客户选择竞品', 'OPPORTUNITY_FAIL', '100001',
UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
INSERT INTO sys_dict values (UUID_SHORT(), '立项失败', 'OPPORTUNITY_FAIL', '100001',
UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
INSERT INTO sys_dict values (UUID_SHORT(), '决策链复杂', 'OPPORTUNITY_FAIL', '100001',
UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
INSERT INTO sys_dict values (UUID_SHORT(), '预算限制', 'OPPORTUNITY_FAIL', '100001',
UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
INSERT INTO sys_dict values (UUID_SHORT(), '需求变化', 'OPPORTUNITY_FAIL', '100001',
UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

-- opportunity fail reason => sys_dict.fail.id
UPDATE opportunity set failure_reason = (
    SELECT sd.id FROM sys_dict sd WHERE sd.type = 'OPPORTUNITY_FAIL' and sd.organization_id = '100001' AND sd.name = failure_reason
) WHERE failure_reason IS NOT NULL;

SET SESSION innodb_lock_wait_timeout = DEFAULT;