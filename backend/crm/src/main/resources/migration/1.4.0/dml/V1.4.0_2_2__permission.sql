-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:APPROVAL');


SET SESSION innodb_lock_wait_timeout = DEFAULT;