-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化仪表板菜单
insert into sys_module value (UUID_SHORT(), '100001', 'dashboard', false, 7,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

INSERT INTO `dashboard_module`(`id`, `organization_id`, `name`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    (UUID_SHORT(), '100001', '默认文件夹', 'NONE', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;