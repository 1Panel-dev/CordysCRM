SET SESSION innodb_lock_wait_timeout = 7200;

-- Drop contract payment plan business field
ALTER TABLE contract_payment_plan
    DROP COLUMN plan_amount,
    DROP COLUMN plan_end_time;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
