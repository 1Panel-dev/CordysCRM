-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Remove phone prefix on system field
UPDATE clue SET phone = IF(phone LIKE '(+%)%', SUBSTRING(phone, LOCATE(')', phone) + 1), phone)
WHERE phone LIKE '(+%)%';
UPDATE customer_contact SET phone = IF(phone LIKE '(+%)%', SUBSTRING(phone, LOCATE(')', phone) + 1), phone)
WHERE phone LIKE '(+%)%';

-- Remove phone prefix on custom field
UPDATE clue_field cf JOIN sys_module_field smf ON cf.field_id = smf.id AND smf.type = 'PHONE'
SET cf.field_value = IF(cf.field_value LIKE '(+%)%', SUBSTRING(cf.field_value, LOCATE(')', cf.field_value) + 1), cf.field_value)
WHERE cf.field_value LIKE '(+%)%';

UPDATE customer_field cf JOIN sys_module_field smf ON cf.field_id = smf.id AND smf.type = 'PHONE'
SET cf.field_value = IF(cf.field_value LIKE '(+%)%', SUBSTRING(cf.field_value, LOCATE(')', cf.field_value) + 1), cf.field_value)
WHERE cf.field_value LIKE '(+%)%';

UPDATE customer_contact_field ccf JOIN sys_module_field smf ON ccf.field_id = smf.id AND smf.type = 'PHONE'
SET ccf.field_value = IF(ccf.field_value LIKE '(+%)%', SUBSTRING(ccf.field_value, LOCATE(')', ccf.field_value) + 1), ccf.field_value)
WHERE ccf.field_value LIKE '(+%)%';

UPDATE opportunity_field opf JOIN sys_module_field smf ON opf.field_id = smf.id AND smf.type = 'PHONE'
SET opf.field_value = IF(opf.field_value LIKE '(+%)%', SUBSTRING(opf.field_value, LOCATE(')', opf.field_value) + 1), opf.field_value)
WHERE opf.field_value LIKE '(+%)%';

UPDATE product_field pf JOIN sys_module_field smf ON pf.field_id = smf.id AND smf.type = 'PHONE'
SET pf.field_value = IF(pf.field_value LIKE '(+%)%', SUBSTRING(pf.field_value, LOCATE(')', pf.field_value) + 1), pf.field_value)
WHERE pf.field_value LIKE '(+%)%';

UPDATE follow_up_plan_field fupf JOIN sys_module_field smf ON fupf.field_id = smf.id AND smf.type = 'PHONE'
SET fupf.field_value = IF(fupf.field_value LIKE '(+%)%', SUBSTRING(fupf.field_value, LOCATE(')', fupf.field_value) + 1), fupf.field_value)
WHERE fupf.field_value LIKE '(+%)%';

UPDATE follow_up_record_field furf JOIN sys_module_field smf ON furf.field_id = smf.id AND smf.type = 'PHONE'
SET furf.field_value = IF(furf.field_value LIKE '(+%)%', SUBSTRING(furf.field_value, LOCATE(')', furf.field_value) + 1), furf.field_value)
WHERE furf.field_value LIKE '(+%)%';

SET SESSION innodb_lock_wait_timeout = DEFAULT;