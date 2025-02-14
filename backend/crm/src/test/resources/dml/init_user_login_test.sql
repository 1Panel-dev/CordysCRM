-- 初始化一个系统管理员

INSERT INTO `sys_user` (`id`,
                        `name`,
                        `email`,
                        `password`,
                        `gender`,
                        `phone`,
                        `language`,
                        `last_organization_id`,
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
        'zh_CN',
        '100001',
        1716175907000,
        1729752373360,
        '717345437786112',
        'test.login');


INSERT INTO `sys_organization_user`(`id`, `organization_id`, `department_id`, `resource_user_id`, `user_id`, `enable`, `employee_id`, `position`, `employee_type`, `supervisor_id`, `work_city`, `create_user`, `update_user`, `create_time`, `update_time`)
VALUES ('test', '100001', '100001', '', 'admin', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000);

