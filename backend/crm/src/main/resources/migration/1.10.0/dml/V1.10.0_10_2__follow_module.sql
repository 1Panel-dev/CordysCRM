SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), '100001', 'follow', true, 11, 'admin', UNIX_TIMESTAMP() * 1000, 'admin',
        UNIX_TIMESTAMP() * 1000);


SET SESSION innodb_lock_wait_timeout = DEFAULT;