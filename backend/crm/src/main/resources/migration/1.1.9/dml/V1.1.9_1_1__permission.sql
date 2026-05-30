-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init import permissions for admin roles

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PERSONAL_API_KEY:READ');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PERSONAL_API_KEY:ADD');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PERSONAL_API_KEY:UPDATE');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PERSONAL_API_KEY:DELETE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;