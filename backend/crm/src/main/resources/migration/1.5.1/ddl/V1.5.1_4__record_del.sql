-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Delete payment record bank column
ALTER TABLE contract_payment_record DROP COLUMN record_bank;
ALTER TABLE contract_payment_record DROP COLUMN record_bank_no;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;