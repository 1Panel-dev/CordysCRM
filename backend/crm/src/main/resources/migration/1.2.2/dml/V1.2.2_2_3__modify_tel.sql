-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Init opportunity pos
UPDATE opportunity
SET pos = (SELECT rn
           FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn
                 FROM opportunity) t2
           WHERE t2.id = opportunity.id);

SET SESSION innodb_lock_wait_timeout = DEFAULT;