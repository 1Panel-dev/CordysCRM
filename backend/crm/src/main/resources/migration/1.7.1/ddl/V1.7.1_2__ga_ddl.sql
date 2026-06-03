-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify sys_module_field_blob prop column to longtext
ALTER TABLE contract MODIFY COLUMN start_time bigint NULL;
ALTER TABLE contract MODIFY COLUMN end_time bigint NULL;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'business_title'
              AND COLUMN_NAME = 'company_number'
        ),
        'SELECT 1',
        'ALTER TABLE business_title ADD COLUMN company_number BIGINT NOT NULL AUTO_INCREMENT UNIQUE COMMENT ''公司编号'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;