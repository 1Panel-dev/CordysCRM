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

UPDATE opportunity o
    JOIN (
    SELECT 'CREATE' AS code, c.id AS new_stage_id
    FROM opportunity_stage_config c
    WHERE c.name = '新建'
    UNION ALL
    SELECT 'CLEAR_REQUIREMENTS', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '需求明确'
    UNION ALL
    SELECT 'SCHEME_VALIDATION', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '方案验证'
    UNION ALL
    SELECT 'PROJECT_PROPOSAL_REPORT', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '立项汇报'
    UNION ALL
    SELECT 'BUSINESS_PROCUREMENT', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '商务采购'
    UNION ALL
    SELECT 'SUCCESS', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '成功'
    UNION ALL
    SELECT 'FAIL', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '失败'
    ) m
ON o.stage = m.code
    SET o.stage = m.new_stage_id;

UPDATE opportunity o
    JOIN (
    SELECT 'CREATE' AS code, c.id AS new_stage_id
    FROM opportunity_stage_config c
    WHERE c.name = '新建'
    UNION ALL
    SELECT 'CLEAR_REQUIREMENTS', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '需求明确'
    UNION ALL
    SELECT 'SCHEME_VALIDATION', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '方案验证'
    UNION ALL
    SELECT 'PROJECT_PROPOSAL_REPORT', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '立项汇报'
    UNION ALL
    SELECT 'BUSINESS_PROCUREMENT', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '商务采购'
    UNION ALL
    SELECT 'SUCCESS', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '成功'
    UNION ALL
    SELECT 'FAIL', c.id
    FROM opportunity_stage_config c
    WHERE c.name = '失败'
    ) m
ON o.last_stage = m.code
    SET o.last_stage = m.new_stage_id;

SET SESSION innodb_lock_wait_timeout = DEFAULT;