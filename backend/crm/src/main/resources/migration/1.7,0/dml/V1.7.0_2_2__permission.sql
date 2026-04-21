-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init org_admin permissions for approval flow
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'APPROVAL_FLOW:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'APPROVAL_FLOW:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'APPROVAL_FLOW:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'APPROVAL_FLOW:DELETE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;