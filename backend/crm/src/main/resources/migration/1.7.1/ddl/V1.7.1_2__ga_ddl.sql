-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify sys_module_field_blob prop column to longtext
ALTER TABLE contract MODIFY COLUMN start_time bigint NULL;
ALTER TABLE contract MODIFY COLUMN end_time bigint NULL;

ALTER TABLE business_title ADD COLUMN company_number BIGINT NOT NULL AUTO_INCREMENT UNIQUE;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;