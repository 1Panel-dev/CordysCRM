-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE approval_flow(
  `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
  `current_version_id` VARCHAR(32) NOT NULL   COMMENT '当前版本ID' ,
  `number` VARCHAR(50) NOT NULL   COMMENT '流程编码;流程编码，自增，格式：CTR-APV-001（合同），INV-APV-001（发票），ORD-APV-001（订单）' ,
  `name` VARCHAR(255) NOT NULL   COMMENT '流程名称;流程名称' ,
  `form_type` VARCHAR(50) NOT NULL   COMMENT '表单类型;表单类型：QUOTATION(报价)、CONTRACT(合同)、INVOICE(发票)、ORDER(订单)' ,
  `create_execute` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '新建时执行;新建时是否执行审批流' ,
  `update_execute` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '编辑时执行;编辑时是否执行审批流' ,
  `submitter_can_revoke` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '允许提交人撤销;允许提交人撤销审批中的申请' ,
  `allow_batch_process` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许批量处理;允许审批人批量处理此流程的多个任务' ,
  `allow_withdraw` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许撤回;允许审批人撤回审批' ,
  `allow_add_sign` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许加签' ,
  `duplicate_approver_rule` VARCHAR(20) NOT NULL  DEFAULT 'FIRST_ONLY' COMMENT '重复审批人：FIRST_ONLY/SEQUENTIAL_ALL/EACH' ,
  `require_comment` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '是否必须填写审批意见' ,
  `enable` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '启用状态：0-禁用，1-启用' ,
  `deleted` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除' ,
  `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
  `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
  `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
  `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
  `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
  `status_permissions` VARCHAR(4000) NOT NULL   COMMENT '状态权限配置（JSON格式）' ,
  `description` VARCHAR(3000)    COMMENT '流程描述' ,
  PRIMARY KEY (id)
)  COMMENT = '审批流主表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE approval_flow_version(
  `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
  `flow_id` VARCHAR(32) NOT NULL   COMMENT '审批流ID' ,
  `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
  `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
  `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
  PRIMARY KEY (id)
)  COMMENT = '审批流版本表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_id ON approval_flow_version(flow_id ASC);

CREATE TABLE approval_node(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `flow_version_id` VARCHAR(32) NOT NULL   COMMENT '审批流版本ID' ,
    `number` VARCHAR(50) NOT NULL   COMMENT '节点编码;格式：PN001、PN002' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '节点名称' ,
    `node_type` VARCHAR(50) NOT NULL   COMMENT '节点类型：START\CONDITION\DEFAULT\END' ,
    `sort` INT(11) NOT NULL  DEFAULT 0 COMMENT '排序序号' ,
    PRIMARY KEY (id)
)  COMMENT = '审批节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_version_id ON approval_node(flow_version_id ASC);

CREATE TABLE approval_node_approver(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `flow_version_id` VARCHAR(32) NOT NULL   COMMENT '审批流版本ID;流程ID' ,
    `approval_type` VARCHAR(20) NOT NULL  DEFAULT 'AUTO_PASS' COMMENT '审批类型;审批类型：MANUAL(人工审批)、AUTO_PASS(自动通过)、AUTO_REJECT(自动拒绝)' ,
    `multi_approver_mode` VARCHAR(20) NOT NULL   COMMENT '多人审批方式;多人审批方式：ALL(会签)/ANY(或签)/SEQUENTIAL(依次审批)' ,
    `empty_approver_action` VARCHAR(20) NOT NULL  DEFAULT 'AUTO_PASS' COMMENT '审批人为空时动作：AUTO_PASS(自动通过)/ASSIGN_SPECIFIC(指定人员审批)/ASSIGN_ADMIN(转交给审批管理员)' ,
    `fallback_approver` VARCHAR(32) COMMENT '审批人为空时，兜底审批人',
    `same_submitter_action` VARCHAR(20) NOT NULL  DEFAULT 'SKIP' COMMENT '审批人与提交人相同时动作：SKIP(自动跳过)/ALLOW(由提交人审批)/ASSIGN_SUPERIOR(转交给直属上级审批)' ,
    `approver_type` VARCHAR(50)  COMMENT '审批人类型' ,
    `approver_direction` VARCHAR(20) NULL DEFAULT 'BOTTOM_UP' COMMENT '审批人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，仅MULTIPLE_SUPERIOR/MULTIPLE_DEPT_HEAD生效' ,
    `cc_type` VARCHAR(50) COMMENT '抄送人类型' ,
    `cc_direction` VARCHAR(20) NULL DEFAULT 'BOTTOM_UP' COMMENT '抄送人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，仅MULTIPLE_SUPERIOR/MULTIPLE_DEPT_HEAD生效' ,
    `cc_list` text COMMENT '抄送人列表（JSON数组）' ,
    `approver_list` text COMMENT '审批人列表（JSON数组）' ,
    `pass_post_config` VARCHAR(4000) COMMENT '审批通过后配置（JSON格式）' ,
    `reject_post_config` VARCHAR(4000) COMMENT '审批驳回后配置（JSON格式）' ,
    `field_permissions` VARCHAR(4000) COMMENT '字段权限配置（JSON格式）' ,
    PRIMARY KEY (id)
)  COMMENT = '审批人节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_version_id ON approval_node_approver(flow_version_id ASC);

CREATE TABLE approval_node_condition(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `flow_version_id` VARCHAR(32) NOT NULL COMMENT '审批流版本ID' ,
    `condition_config` VARCHAR(4000)  COMMENT '条件配置JSON' ,
    PRIMARY KEY (id)
)  COMMENT = '条件节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_version_id ON approval_node_condition(flow_version_id ASC);

CREATE TABLE approval_node_link(
    `id` VARCHAR(32) NOT NULL   COMMENT '主键' ,
    `flow_version_id` VARCHAR(32) NOT NULL   COMMENT '审批流版本ID' ,
    `from_node_id` VARCHAR(32) NOT NULL   COMMENT '源节点ID' ,
    `to_node_id` VARCHAR(32) NOT NULL   COMMENT '目标节点ID' ,
    `sort` INT NOT NULL  DEFAULT 0 COMMENT '分支评估顺序' ,
    PRIMARY KEY (id)
)  COMMENT = '节点连接表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_version_id_from_id ON approval_node_link(flow_version_id ASC,from_node_id ASC);

CREATE TABLE approval_instance(
    `id` VARCHAR(32) NOT NULL COMMENT 'ID' ,
    `flow_version_id` VARCHAR(32) NOT NULL   COMMENT '审批流版本ID' ,
    `type` VARCHAR(20) NOT NULL   COMMENT '表单类型' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '审批的业务数据ID' ,
    `submitter_id` VARCHAR(32) NOT NULL   COMMENT '提交人ID' ,
    `current_node_id` VARCHAR(32)    COMMENT '当前节点ID' ,
    `approval_status` VARCHAR(20) NOT NULL   COMMENT '审批状态' ,
    `submit_time` BIGINT    COMMENT '提审时间' ,
    `approval_time` BIGINT    COMMENT '审批完成时间' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '审批实例表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_version_id ON approval_instance(flow_version_id ASC);
CREATE INDEX idx_resource_id ON approval_instance(resource_id ASC);
CREATE INDEX idx_submitter_id ON approval_instance(submitter_id ASC);

CREATE TABLE approval_task(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `node_id` VARCHAR(32) NOT NULL   COMMENT '节点ID' ,
    `node_round` INT NOT NULL   COMMENT '节点轮次' ,
    `instance_id` VARCHAR(32) NOT NULL   COMMENT '审批实例ID' ,
    `approver_id` VARCHAR(20) NOT NULL   COMMENT '审批人ID' ,
    `status` VARCHAR(20) NOT NULL   COMMENT '任务状态; 待审批: PENDING, 审批中: APPROVING, 已通过: APPROVED, 已驳回: UNAPPROVED, 已撤销: REVOKED' ,
    `type` VARCHAR(20)    COMMENT '任务类型; 抄送: CC; 加签: SN; 退回: BK; 普通: NL;' ,
    `action` VARCHAR(20)    COMMENT '执行操作; 同意: APPROVE;驳回: REJECT;加签: SIGN;退回: BACK;' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '审批任务表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_instance_node_id ON approval_task(instance_id ASC,node_id ASC);
CREATE INDEX idx_approver_id ON approval_task(approver_id ASC);

CREATE TABLE approval_add_sign_task(
    `id` VARCHAR(32) NOT NULL   COMMENT '主键ID' ,
    `type` VARCHAR(20) NOT NULL   COMMENT '加签方式;{before: 在我之前、after: 在我之后' ,
    `task_id` VARCHAR(32) NOT NULL   COMMENT '加签任务ID' ,
    `sign_task_id` VARCHAR(32) NOT NULL   COMMENT '被加签的任务ID' ,
    `root_task_id` VARCHAR(32)    COMMENT '根任务ID' ,
    `sort` BIGINT NOT NULL   COMMENT '顺序;排序越小越往前' ,
    `comment` TEXT    COMMENT '加签意见' ,
    PRIMARY KEY (id)
)  COMMENT = '加签任务表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_task_id ON approval_add_sign_task(task_id ASC);
CREATE INDEX idx_sign_task_id ON approval_add_sign_task(sign_task_id ASC);
CREATE INDEX idx_root_task_id ON approval_add_sign_task(root_task_id ASC);

CREATE TABLE approval_return_back_record(
    `id` VARCHAR(32) NOT NULL   COMMENT '主键ID' ,
    `instance_id` VARCHAR(32) NOT NULL   COMMENT '审批实例ID' ,
    `task_id` VARCHAR(32) NOT NULL   COMMENT '当前任务ID' ,
    `return_to_node_id` VARCHAR(32) NOT NULL   COMMENT '退回至任务ID' ,
    `return_reason` TEXT    COMMENT '退回原因' ,
    `return_user_id` VARCHAR(32) NOT NULL   COMMENT '退回操作人' ,
    PRIMARY KEY (id)
)  COMMENT = '退回记录表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_instance_id ON approval_return_back_record(instance_id ASC);

CREATE TABLE approval_record(
    `id` VARCHAR(32) NOT NULL COMMENT 'ID' ,
    `instance_id` VARCHAR(32) NOT NULL   COMMENT '审批实例ID' ,
    `task_id` VARCHAR(32)    COMMENT '任务ID' ,
    `node_id` VARCHAR(32) NOT NULL   COMMENT '节点ID' ,
    `node_round` INT NOT NULL   COMMENT '节点轮次' ,
    `result` VARCHAR(255)    COMMENT '审批结果' ,
    `comment` TEXT    COMMENT '审批意见' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '审批记录表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_instance_task_id ON approval_record(instance_id ASC,task_id ASC);

CREATE TABLE approval_instance_attachment(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `instance_id` VARCHAR(32) NOT NULL   COMMENT '审批实例ID' ,
    `element_id` VARCHAR(32) NOT NULL   COMMENT '审批节点ID' ,
    `attachment_id` VARCHAR(32) NOT NULL   COMMENT '附件ID' ,
    PRIMARY KEY (id)
)  COMMENT = '审批实例附件表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_instance_id ON approval_instance_attachment(instance_id ASC);
CREATE INDEX idx_element_id ON approval_instance_attachment(element_id ASC);


CREATE TABLE contract_stage_config
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '合同状态',
    `type`            VARCHAR(50)  NOT NULL COMMENT '状态类型',
    `afoot_roll_back` BIT(1) DEFAULT 0 COMMENT '进行中回退设置',
    `end_roll_back`   BIT(1) DEFAULT 0 COMMENT '完结回退设置',
    `pos`             BIGINT       NOT NULL COMMENT '顺序',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '合同状态流设置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


ALTER TABLE contract ADD COLUMN pos BIGINT DEFAULT NULL;

ALTER TABLE sales_order ADD COLUMN pos BIGINT DEFAULT NULL;

-- add order approval status
ALTER TABLE sales_order ADD COLUMN approval_status VARCHAR(50) NOT NULL COMMENT '审批状态' AFTER stage;

-- add quotation invalid
ALTER TABLE opportunity_quotation ADD COLUMN invalid TINYINT(1) DEFAULT 0 COMMENT '是否作废: 0-正常, 1-作废' AFTER approval_status;

-- drop quotation approval
DROP TABLE opportunity_quotation_approval;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;