-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO business_title_config (id, field, required, organization_id)
VALUES (UUID_SHORT(), 'remark', false, '100001');


SET SESSION innodb_lock_wait_timeout = DEFAULT;