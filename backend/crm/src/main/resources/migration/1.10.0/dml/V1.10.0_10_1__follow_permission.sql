SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:READ'), (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:ADD'),
       (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:UPDATE'), (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:DELETE'),
       (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:IMPORT'), (UUID_SHORT(), 'org_admin', 'FOLLOW_PLAN:EXPORT'),
       (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:READ'), (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:ADD'),
       (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:UPDATE'), (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:DELETE'),
       (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:IMPORT'), (UUID_SHORT(), 'org_admin', 'FOLLOW_RECORD:EXPORT'),
       (UUID_SHORT(), 'sales_manager', 'FOLLOW_PLAN:READ'), (UUID_SHORT(), 'sales_manager', 'FOLLOW_PLAN:ADD'),
       (UUID_SHORT(), 'sales_manager', 'FOLLOW_PLAN:UPDATE'), (UUID_SHORT(), 'sales_manager', 'FOLLOW_PLAN:DELETE'),
       (UUID_SHORT(), 'sales_manager', 'FOLLOW_RECORD:READ'), (UUID_SHORT(), 'sales_manager', 'FOLLOW_RECORD:ADD'),
       (UUID_SHORT(), 'sales_manager', 'FOLLOW_RECORD:UPDATE'), (UUID_SHORT(), 'sales_manager', 'FOLLOW_RECORD:DELETE'),
       (UUID_SHORT(), 'sales_staff', 'FOLLOW_PLAN:READ'), (UUID_SHORT(), 'sales_staff', 'FOLLOW_PLAN:ADD'),
       (UUID_SHORT(), 'sales_staff', 'FOLLOW_PLAN:UPDATE'), (UUID_SHORT(), 'sales_staff', 'FOLLOW_PLAN:DELETE'),
       (UUID_SHORT(), 'sales_staff', 'FOLLOW_RECORD:READ'), (UUID_SHORT(), 'sales_staff', 'FOLLOW_RECORD:ADD'),
       (UUID_SHORT(), 'sales_staff', 'FOLLOW_RECORD:UPDATE'), (UUID_SHORT(), 'sales_staff', 'FOLLOW_RECORD:DELETE');


SET SESSION innodb_lock_wait_timeout = DEFAULT;