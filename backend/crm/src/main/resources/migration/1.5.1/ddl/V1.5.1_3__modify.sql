-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Modify quotation_prop column from TEXT to LONGTEXT in opportunity_quotation_snapshot table
ALTER TABLE opportunity_quotation_snapshot
    MODIFY COLUMN quotation_prop LONGTEXT NULL COMMENT '表单属性快照';
ALTER TABLE contract_snapshot
    MODIFY contract_prop LONGTEXT NULL COMMENT '表单属性快照';
ALTER TABLE contract_invoice_snapshot
    MODIFY invoice_prop LONGTEXT NULL COMMENT '表单属性快照';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;