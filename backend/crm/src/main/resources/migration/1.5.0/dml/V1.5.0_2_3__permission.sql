-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Init contract payment record permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_RECORD:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:IMPORT');



INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_RECORD:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:UPDATE');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_RECORD:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_BUSINESS_TITLE:UPDATE');


-- Init invoice permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_INVOICE:APPROVAL');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_INVOICE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_INVOICE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_INVOICE:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_INVOICE:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_INVOICE:EXPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_INVOICE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_INVOICE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_INVOICE:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_INVOICE:EXPORT');

-- init contract payment operate permission
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:PAYMENT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:PAYMENT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:PAYMENT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;