-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- process quotation voided status
update opportunity_quotation set invalid = CASE WHEN approval_status = 'VOIDED' THEN 1 ELSE 0 END;

-- process approval status
update contract set approval_status = 'NONE' where approval_status = 'APPROVING';
update opportunity_quotation set approval_status = 'NONE' where approval_status in ('APPROVING', 'VOIDED');
update contract_invoice set approval_status = 'NONE' where approval_status = 'APPROVING';
update sales_order set approval_status = 'NONE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;