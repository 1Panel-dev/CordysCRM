-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'DASHBOARD:READ'),
       (UUID_SHORT(), 'org_admin', 'DASHBOARD:ADD'),
       (UUID_SHORT(), 'org_admin', 'DASHBOARD:EDIT'),
       (UUID_SHORT(), 'org_admin', 'DASHBOARD:DELETE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;