-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

insert into sys_module value (UUID_SHORT(), '100001', 'order', true, 11,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

INSERT INTO `sales_order_stage_config`(`id`, `name`, `type`, `afoot_roll_back`, `end_roll_back`, `pos`, `organization_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('CREATE', '新建', 'AFOOT', b'1', b'0', 1, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('PENDING_SHIPMENT', '待发货', 'AFOOT', b'1', b'0', 2, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('PARTIALLY_SHIPPED', '部分发货', 'AFOOT', b'1', b'0', 3, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('SHIPPED', '已发货', 'AFOOT', b'1', b'0', 4, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('PENDING_ACCEPTANCE', '待验收', 'AFOOT', b'1', b'0', 5, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('COMPLETED', '已完成', 'END', b'1', b'0', 6, '100001', 1772521969413, 1772521969413, 'admin', 'admin'),
    ('VOIDED', '已作废', 'END', b'1', b'0', 7, '100001', 1772521969413, 1772521969413, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;