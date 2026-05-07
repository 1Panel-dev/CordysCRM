-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Init contract pos
UPDATE contract
SET pos = (SELECT rn
           FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn
                 FROM contract) t2
           WHERE t2.id = contract.id);


UPDATE sales_order
SET pos = (SELECT rn
           FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn
                 FROM sales_order) t2
           WHERE t2.id = sales_order.id);

SET SESSION innodb_lock_wait_timeout = DEFAULT;