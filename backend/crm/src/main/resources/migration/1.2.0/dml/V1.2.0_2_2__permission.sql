-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'AGENT:READ'),
       (UUID_SHORT(), 'org_admin', 'AGENT:ADD'),
       (UUID_SHORT(), 'org_admin', 'AGENT:UPDATE'),
       (UUID_SHORT(), 'org_admin', 'AGENT:DELETE');

-- init product import permission
INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'PRODUCT_MANAGEMENT:IMPORT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;