-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CLUE_MANAGEMENT:TRANSFER');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CUSTOMER_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CLUE_MANAGEMENT:TRANSFER');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CUSTOMER_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_MANAGEMENT:TRANSFER');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CLUE_MANAGEMENT:TRANSFER');

SET SESSION innodb_lock_wait_timeout = DEFAULT;