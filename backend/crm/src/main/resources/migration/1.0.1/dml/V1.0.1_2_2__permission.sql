-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 管理员
SET @opportunity_resignID = UUID_SHORT();-- 初始化OPPORTUNITY_MANAGEMENT:RESIGN权限
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES (@opportunity_resignID, 'org_admin', 'OPPORTUNITY_MANAGEMENT:RESIGN');

SET @customer_exportOrgID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@customer_exportOrgID, 'org_admin', 'CUSTOMER_MANAGEMENT:EXPORT');

SET @opportunity_exportOrgID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@opportunity_exportOrgID, 'org_admin', 'OPPORTUNITY_MANAGEMENT:EXPORT');

SET @clue_exportOrgID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ( @clue_exportOrgID, 'org_admin', 'CLUE_MANAGEMENT:EXPORT');

-- 销售经理
SET @customer_exportManID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@customer_exportManID, 'sales_manager', 'CUSTOMER_MANAGEMENT:EXPORT');

SET @opportunity_exportManID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@opportunity_exportManID, 'sales_manager', 'OPPORTUNITY_MANAGEMENT:EXPORT');

SET @clue_exportManID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@clue_exportManID, 'sales_manager', 'CLUE_MANAGEMENT:EXPORT');

-- 销售专员
SET @customer_exportStaID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES ('1117962807492627', 'sales_staff', 'CUSTOMER_MANAGEMENT:EXPORT');

SET @opportunity_exportStaID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@opportunity_exportStaID, 'sales_staff', 'OPPORTUNITY_MANAGEMENT:EXPORT');

SET @clue_exportStaID = UUID_SHORT();
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (@clue_exportStaID, 'sales_staff', 'CLUE_MANAGEMENT:EXPORT');



SET SESSION innodb_lock_wait_timeout = DEFAULT;