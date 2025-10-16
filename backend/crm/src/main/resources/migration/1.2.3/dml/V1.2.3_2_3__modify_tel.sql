-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


DELETE FROM sys_organization_config WHERE type = 'AUTH';

DELETE FROM sys_organization_config_detail WHERE type in ('WE_COM_OAUTH2', 'WECOM_OAUTH2', 'DINGTALK_OAUTH2', 'WECOM_NOTICE', 'DINGTALK_NOTICE', 'WECOM_CODE', 'DINGTALK_CODE');



SET SESSION innodb_lock_wait_timeout = DEFAULT;