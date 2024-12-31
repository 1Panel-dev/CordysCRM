-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化用户
INSERT INTO `sys_user` (`id`,
                        `name`,
                        `email`,
                        `password`,
                        `enable`,
                        `phone`,
                        `create_time`,
                        `update_time`,
                        `create_user`,
                        `update_user`)
VALUES ('admin',
        'Administrator',
        'admin@cordys-crm.io',
        MD5('CordysCRM'),
        1,
        '4000520755',
        1716175907000,
        1729752373360,
        '717345437786112',
        'admin');
-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
