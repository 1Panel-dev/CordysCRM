-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CLUE_MANAGEMENT:IMPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;