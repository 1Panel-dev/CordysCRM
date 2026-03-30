-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE business_title
    ADD `city` VARCHAR(255) COMMENT '市';

ALTER TABLE business_title
    CHANGE COLUMN area province VARCHAR (255) COMMENT '省';


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;