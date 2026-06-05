-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init org_admin permissions for custom form
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CUSTOM_FORM:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CUSTOM_FORM:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CUSTOM_FORM:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CUSTOM_FORM:READ');

SET SESSION innodb_lock_wait_timeout = DEFAULT;