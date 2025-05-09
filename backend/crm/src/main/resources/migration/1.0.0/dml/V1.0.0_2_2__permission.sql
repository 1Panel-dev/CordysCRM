-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 管理员
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142848', 'org_admin', 'SYS_ORGANIZATION:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142849', 'org_admin', 'SYS_ORGANIZATION:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142850', 'org_admin', 'SYS_ORGANIZATION:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142851', 'org_admin', 'SYS_ORGANIZATION:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142852', 'org_admin', 'SYS_ORGANIZATION:IMPORT');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142853', 'org_admin', 'SYS_ORGANIZATION:SYNC');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142854', 'org_admin', 'SYS_ORGANIZATION_USER:RESET_PASSWORD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142855', 'org_admin', 'SYSTEM_ROLE:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142856', 'org_admin', 'SYSTEM_ROLE:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142857', 'org_admin', 'SYSTEM_ROLE:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142858', 'org_admin', 'SYSTEM_ROLE:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142859', 'org_admin', 'SYSTEM_ROLE:ADD_USER');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142860', 'org_admin', 'SYSTEM_ROLE:REMOVE_USER');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142861', 'org_admin', 'MODULE_SETTING:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142862', 'org_admin', 'MODULE_SETTING:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142863', 'org_admin', 'SYSTEM_NOTICE:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142864', 'org_admin', 'SYSTEM_NOTICE:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142865', 'org_admin', 'SYSTEM_NOTICE:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142866', 'org_admin', 'SYSTEM_NOTICE:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142867', 'org_admin', 'SYSTEM_SETTING:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142868', 'org_admin', 'SYSTEM_SETTING:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142869', 'org_admin', 'CUSTOMER_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142870', 'org_admin', 'CUSTOMER_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142871', 'org_admin', 'CUSTOMER_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142872', 'org_admin', 'CUSTOMER_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142873', 'org_admin', 'CUSTOMER_MANAGEMENT:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142874', 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142875', 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142876', 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:ASSIGN');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142877', 'org_admin', 'CUSTOMER_MANAGEMENT_POOL:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142878', 'org_admin', 'CUSTOMER_MANAGEMENT_CONTACT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142879', 'org_admin', 'CUSTOMER_MANAGEMENT_CONTACT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142880', 'org_admin', 'CUSTOMER_MANAGEMENT_CONTACT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142881', 'org_admin', 'CUSTOMER_MANAGEMENT_CONTACT:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142882', 'org_admin', 'CLUE_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142883', 'org_admin', 'CLUE_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142884', 'org_admin', 'CLUE_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142885', 'org_admin', 'CLUE_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142886', 'org_admin', 'CLUE_MANAGEMENT:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142887', 'org_admin', 'CLUE_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142888', 'org_admin', 'CLUE_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142889', 'org_admin', 'CLUE_MANAGEMENT_POOL:ASSIGN');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142890', 'org_admin', 'CLUE_MANAGEMENT_POOL:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142891', 'org_admin', 'OPPORTUNITY_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142892', 'org_admin', 'OPPORTUNITY_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142893', 'org_admin', 'OPPORTUNITY_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142894', 'org_admin', 'OPPORTUNITY_MANAGEMENT:DELETE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142895', 'org_admin', 'PRODUCT_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142896', 'org_admin', 'PRODUCT_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142897', 'org_admin', 'PRODUCT_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1076902920142898', 'org_admin', 'PRODUCT_MANAGEMENT:DELETE');
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1076902920142899', 'org_admin', 'OPERATION_LOG:READ');
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1076902920142900', 'org_admin', 'SYSTEM_SETTING:ADD');
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1076902920142901', 'org_admin', 'SYSTEM_SETTING:DELETE');

-- 销售经理
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085312', 'sales_manager', 'CUSTOMER_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085313', 'sales_manager', 'CUSTOMER_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085314', 'sales_manager', 'CUSTOMER_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085315', 'sales_manager', 'CUSTOMER_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085316', 'sales_manager', 'CUSTOMER_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120642867085317', 'sales_manager', 'CUSTOMER_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954496', 'sales_manager', 'CUSTOMER_MANAGEMENT_CONTACT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954497', 'sales_manager', 'CUSTOMER_MANAGEMENT_CONTACT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954498', 'sales_manager', 'CUSTOMER_MANAGEMENT_CONTACT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954499', 'sales_manager', 'CLUE_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954500', 'sales_manager', 'CLUE_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954501', 'sales_manager', 'CLUE_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954502', 'sales_manager', 'CUSTOMER_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954503', 'sales_manager', 'CLUE_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954504', 'sales_manager', 'CLUE_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954505', 'sales_manager', 'OPPORTUNITY_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954506', 'sales_manager', 'OPPORTUNITY_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954507', 'sales_manager', 'OPPORTUNITY_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1120660046954508', 'sales_manager', 'CLUE_MANAGEMENT:RECYCLE');

-- 销售专员
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492608', 'sales_staff', 'CUSTOMER_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492609', 'sales_staff', 'CUSTOMER_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492610', 'sales_staff', 'CUSTOMER_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492611', 'sales_staff', 'CUSTOMER_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492612', 'sales_staff', 'CUSTOMER_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492613', 'sales_staff', 'CUSTOMER_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492614', 'sales_staff', 'CUSTOMER_MANAGEMENT_CONTACT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492615', 'sales_staff', 'CUSTOMER_MANAGEMENT_CONTACT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492616', 'sales_staff', 'CUSTOMER_MANAGEMENT_CONTACT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492617', 'sales_staff', 'CLUE_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492618', 'sales_staff', 'CLUE_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492619', 'sales_staff', 'CLUE_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492620', 'sales_staff', 'CUSTOMER_MANAGEMENT:RECYCLE');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492621', 'sales_staff', 'CLUE_MANAGEMENT_POOL:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492622', 'sales_staff', 'CLUE_MANAGEMENT_POOL:PICK');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492623', 'sales_staff', 'OPPORTUNITY_MANAGEMENT:READ');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492624', 'sales_staff', 'OPPORTUNITY_MANAGEMENT:ADD');
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1117962807492625', 'sales_staff', 'OPPORTUNITY_MANAGEMENT:UPDATE');
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1117962807492626', 'sales_staff', 'CLUE_MANAGEMENT:RECYCLE');


SET SESSION innodb_lock_wait_timeout = DEFAULT;