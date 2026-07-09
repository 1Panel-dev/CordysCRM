-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 管理员
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CLUE_MANAGEMENT_POOL:IMPORT'),
       (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:IMPORT'),
       (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:IMPORT');



SET SESSION innodb_lock_wait_timeout = DEFAULT;