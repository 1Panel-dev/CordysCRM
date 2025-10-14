-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;


INSERT INTO `opportunity_stage_config`(`id`, `name`, `type`, `rate`, `afoot_roll_back`, `end_roll_back`, `pos`, `organization_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    (UUID_SHORT(), '新建', 'AFOOT', '10', b'1', b'0', 1, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '需求明确', 'AFOOT', '30', b'1', b'0', 2, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '方案验证', 'AFOOT', '50', b'1', b'0', 3, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '立项汇报', 'AFOOT', '70', b'1', b'0', 4, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '商务采购', 'AFOOT', '90', b'1', b'0', 5, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '成功', 'END', '100', b'1', b'0', 6, '100001', 1760175899000, 1760175899000, 'admin', 'admin'),
    (UUID_SHORT(), '失败', 'END', '0', b'1', b'0', 7, '100001', 1760175899000, 1760175899000, 'admin', 'admin');

-- 加入记录/计划顶部导航栏
update sys_navigation set pos = pos + 1 where organization_id = '100001' and pos >= 2;
insert into sys_navigation value (UUID_SHORT(), '100001', 'event', true, 2,
                                  'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

SET SESSION innodb_lock_wait_timeout = DEFAULT;