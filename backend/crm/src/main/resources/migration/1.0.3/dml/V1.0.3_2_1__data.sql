-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化产品排序字段
SET @pos := 0;

UPDATE product
SET pos = (@pos := @pos + 4096)
    ORDER BY id;


SET SESSION innodb_lock_wait_timeout = DEFAULT;