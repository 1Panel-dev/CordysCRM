INSERT INTO `product`(`id`, price, status, `organization_id`, `name`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('11', 3.55,'1','100001', '产品1', 1741255009000, 1741255009000, 'admin', 'admin'),
    ('22', 3.55,'1', '100001', '产品2', 1741255009000, 1741255009000, 'admin', 'admin');

INSERT INTO `customer`(`id`, `name`, `owner`, `collection_time`, `create_time`, `update_time`, `create_user`, `update_user`, `in_shared_pool`, `organization_id`)
VALUES ('123', '1', 'admin', 1741338550027, 1741338550027, 1741338550027, 'admin', 'admin', b'0', '100001');
