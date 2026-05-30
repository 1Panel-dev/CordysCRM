-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init customer merge permission
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT:MERGE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;