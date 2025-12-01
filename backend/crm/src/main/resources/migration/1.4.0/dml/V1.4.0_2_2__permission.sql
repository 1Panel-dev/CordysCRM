-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:APPROVAL');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:VOIDED');
-- init sales manager permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'OPPORTUNITY_QUOTATION:APPROVAL');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:VOIDED');
-- init sales_staff permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_QUOTATION:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_QUOTATION:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_QUOTATION:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_QUOTATION:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'OPPORTUNITY_QUOTATION:VOIDED');

-- init price permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:IMPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRICE:EXPORT');


-- contract
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:ARCHIVE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:VOIDED');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:VOIDED');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:EXPORT');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT:VOIDED');

-- contract_payment_plan
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:DELETE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT_PAYMENT_PLAN:EXPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_PLAN:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_PLAN:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_PLAN:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT_PAYMENT_PLAN:EXPORT');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_PLAN:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_PLAN:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_PLAN:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'sales_staff', 'CONTRACT_PAYMENT_PLAN:EXPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;