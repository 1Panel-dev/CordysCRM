-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


DELETE FROM sys_role_permission WHERE permission_id in ('CONTRACT:ARCHIVE', 'CONTRACT:VOIDED');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:STAGE');

DELETE FROM sys_user_view_condition WHERE name = 'archivedStatus';

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'CONTRACT:DELETE');


UPDATE sys_user_view_condition suc
    JOIN sys_user_view su
ON suc.sys_user_view_id = su.id
    SET suc.NAME = 'stage'
WHERE suc.NAME = 'status'
  AND su.resource_type = 'CONTRACT';

SET SESSION innodb_lock_wait_timeout = DEFAULT;