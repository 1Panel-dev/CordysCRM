-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE business_title
    ADD `area` VARCHAR(255) COMMENT '所属地区';

ALTER TABLE business_title
    ADD `scale` VARCHAR(255) COMMENT '企业规模';

ALTER TABLE business_title
    ADD `industry` VARCHAR(255) COMMENT '国标行业';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;