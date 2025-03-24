INSERT INTO `product`(`id`, `organization_id`, `name`, `create_time`, `update_time`, `create_user`, `update_user`, `price`, `status`)
VALUES
    ('11', '100001', '产品1', 1741255009000, 1741255009000, 'admin', 'admin', '12', '111'),
    ('22', '100001', '产品2', 1741255009000, 1741255009000, 'admin', 'admin', '22', '111');

INSERT INTO `customer`(`id`, `name`, `owner`, `collection_time`, `create_time`, `update_time`, `create_user`, `update_user`, `in_shared_pool`, `organization_id`)
VALUES ('123', '1', 'admin', NULL, 1741338550027, 1741338550027, 'admin', 'admin', b'0', '100001');
