SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE sys_message_task DROP COLUMN email_enable;
ALTER TABLE sys_message_task DROP COLUMN we_com_enable;
ALTER TABLE sys_message_task DROP COLUMN ding_talk_enable;
ALTER TABLE sys_message_task DROP COLUMN lark_enable;
ALTER TABLE sys_message_task MODIFY COLUMN template text NULL COMMENT '消息模版';
ALTER TABLE sys_message_task ADD `name` VARCHAR(500) NOT NULL COMMENT '场景名称';
ALTER TABLE sys_message_task ADD `type` VARCHAR(50) NOT NULL COMMENT '类型(SYSTEM/CUSTOM)';
ALTER TABLE sys_message_task ADD `enable` BIT(1) NOT NULL DEFAULT 1 COMMENT '状态';
ALTER TABLE sys_message_task ADD `scope_id` text NOT NULL COMMENT '范围id';
ALTER TABLE sys_message_task ADD `receive_method` VARCHAR(255) NOT NULL COMMENT '接收方式';
ALTER TABLE sys_notification ADD `trigger_type` VARCHAR(50) NULL COMMENT '触发类型';


ALTER TABLE sys_message_task_config DROP COLUMN task_type;
ALTER TABLE sys_message_task_config ADD `trigger_type` VARCHAR(50) NOT NULL COMMENT '触发类型';
ALTER TABLE sys_message_task_config ADD `message_task_id` VARCHAR(50) NOT NULL COMMENT '消息通知任务id';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
