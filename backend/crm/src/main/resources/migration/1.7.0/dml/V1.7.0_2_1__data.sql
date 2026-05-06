-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO `sales_order_stage_config`(`id`, `name`, `type`, `afoot_roll_back`, `end_roll_back`, `pos`, `organization_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('PENDING_SIGNING', '待签署', 'AFOOT', b'1', b'0', 1, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('SIGNED', '已签署', 'AFOOT', b'1', b'0', 2, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('CHANGE', '合同变更', 'AFOOT', b'1', b'0', 3, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('IN_PROGRESS', '履行中', 'AFOOT', b'1', b'0', 4, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('COMPLETED_PERFORMANCE', '履行完毕', 'AFOOT', b'1', b'0', 5, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('ARCHIVED', '合同完结', 'END', b'1', b'0', 6, '100001', 1776417799080, 1776417799080, 'admin', 'admin'),
    ('VOID', '作废', 'END', b'1', b'0', 7, '100001', 1776417799080, 1776417799080, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;