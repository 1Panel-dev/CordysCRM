-- 初始化一个系统管理员

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
VALUES ('test.login',
        'test.loginistrator',
        'test.login@cordys.io',
        MD5('test.login'),
        1,
        '13651666666',
        1716175907000,
        1729752373360,
        '717345437786112',
        'test.login');