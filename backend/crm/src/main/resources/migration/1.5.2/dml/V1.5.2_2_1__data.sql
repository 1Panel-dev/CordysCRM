-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO business_title_config (id, field, required, organization_id)
VALUES (UUID_SHORT(), 'area', false, '100001'),
       (UUID_SHORT(), 'scale', false, '100001'),
       (UUID_SHORT(), 'industry', false, '100001');

INSERT INTO sys_dict_config (module,organization_id,enabled)
VALUES  ('QUOTATION_APPROVAL','100001',1),
        ('CONTRACT_APPROVAL','100001',1),
        ('INVOICE_APPROVAL','100001',1);

SET SESSION innodb_lock_wait_timeout = DEFAULT;