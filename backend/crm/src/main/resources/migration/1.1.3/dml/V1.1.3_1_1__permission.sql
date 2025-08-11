-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CUSTOMER_MANAGEMENT_POOL:EXPORT');

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CUSTOMER_MANAGEMENT_POOL:EXPORT');

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:EXPORT');


INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CLUE_MANAGEMENT_POOL:EXPORT');

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CLUE_MANAGEMENT_POOL:EXPORT');

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CLUE_MANAGEMENT_POOL:EXPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;