-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Init contract payment record permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:EXPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:EXPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:EXPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;