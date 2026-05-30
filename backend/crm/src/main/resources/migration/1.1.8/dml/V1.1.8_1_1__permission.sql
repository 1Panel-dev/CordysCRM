-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init import permissions for admin roles
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT:IMPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT_CONTACT:IMPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_MANAGEMENT:IMPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;