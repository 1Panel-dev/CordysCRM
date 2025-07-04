-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化仪表板菜单
insert into sys_module value (UUID_SHORT(), '100001', 'dashboard', false, 7,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

SET @internal_dashboard_module_id = UUID_SHORT();
INSERT INTO `dashboard_module`(`id`, `organization_id`, `name`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    (@internal_dashboard_module_id, '100001', '全部仪表板', 'NONE', 0, 1751600572000, 1751600572000, 'admin', 'admin'),
    (UUID_SHORT(), '100001', '默认文件夹', @internal_dashboard_module_id, 4096, 1751600572000, 1751600572000, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;