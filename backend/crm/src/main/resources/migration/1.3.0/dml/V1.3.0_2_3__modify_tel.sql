-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


UPDATE
    sys_organization_config_detail d
SET
    d.config_id = (
        SELECT c.id
        FROM sys_organization_config c
        WHERE c.type = 'THIRD'
    LIMIT 1
    )
WHERE
    d.type = 'WE_COM_OAUTH2'
  AND
    EXISTS (
    SELECT 1
    FROM sys_organization_config c
    WHERE c.type = 'THIRD'
    );

UPDATE sys_organization_config_detail
SET type = 'WECOM_OAUTH2'
WHERE type = 'WE_COM_OAUTH2';

DELETE FROM sys_organization_config WHERE type = 'AUTH';

SET SESSION innodb_lock_wait_timeout = DEFAULT;