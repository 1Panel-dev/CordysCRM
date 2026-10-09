-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 审批人引用用户 ID，需要容纳新增用户的 32 位 UUIDv7。
ALTER TABLE approval_task
    MODIFY COLUMN approver_id VARCHAR(32) NOT NULL COMMENT '审批人ID';

ALTER TABLE opportunity ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '审批状态';
ALTER TABLE opportunity ADD COLUMN approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

CREATE INDEX idx_approval_status ON opportunity (approval_status);

ALTER TABLE clue ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '审批状态';
ALTER TABLE clue ADD COLUMN approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

CREATE INDEX idx_approval_status ON clue (approval_status);

ALTER TABLE customer ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '审批状态';
ALTER TABLE customer ADD COLUMN approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

CREATE INDEX idx_approval_status ON customer (approval_status);


ALTER TABLE sys_role ADD COLUMN pos BIGINT DEFAULT NULL;

UPDATE sys_role
SET pos = (SELECT rn
           FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY create_time) as rn
                 FROM sys_role) t2
           WHERE t2.id = sys_role.id);

-- 待办/已办/我发起的/抄送列表按提审时快照展示资源名称, 不再 join 各业务表。
ALTER TABLE approval_instance
    ADD COLUMN resource_name VARCHAR(255) NULL COMMENT '资源名称(提审时快照)';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
