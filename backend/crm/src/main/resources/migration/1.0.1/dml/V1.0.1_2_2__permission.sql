-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 管理员
INSERT INTO sys_role_permission
    (id, role_id, permission_id)
VALUES ('1120660046954510', 'org_admin', 'OPPORTUNITY_MANAGEMENT:RESIGN');


SET SESSION innodb_lock_wait_timeout = DEFAULT;