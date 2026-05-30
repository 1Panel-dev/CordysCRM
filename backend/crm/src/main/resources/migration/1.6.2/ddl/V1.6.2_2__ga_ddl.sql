-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify sys_module_field_blob prop column to longtext
ALTER TABLE sys_module_field_blob modify column prop longtext;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;