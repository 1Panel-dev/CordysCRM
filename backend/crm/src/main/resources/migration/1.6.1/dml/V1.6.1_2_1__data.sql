-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO business_title_config (id, field, required, organization_id)
VALUES (UUID_SHORT(), 'city', false, '100001'),
       (UUID_SHORT(), 'remark', false, '100001');

update business_title_config set `field` = 'province' where field = 'area' and organization_id = '100001';


SET SESSION innodb_lock_wait_timeout = DEFAULT;