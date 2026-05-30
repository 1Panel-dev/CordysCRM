-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init org_admin permissions for approval flow
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PROCESS_SETTING:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PROCESS_SETTING:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PROCESS_SETTING:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PROCESS_SETTING:DELETE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;