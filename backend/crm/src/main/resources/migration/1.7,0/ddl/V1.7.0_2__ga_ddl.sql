-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE approval_flow(
  `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
  `number` VARCHAR(50) NOT NULL   COMMENT '流程编码;流程编码，自增，格式：CTR-APV-001（合同），INV-APV-001（发票），ORD-APV-001（订单）' ,
  `name` VARCHAR(200) NOT NULL   COMMENT '流程名称;流程名称' ,
  `form_type` VARCHAR(50) NOT NULL   COMMENT '表单类型;表单类型：QUOTATION(报价)、CONTRACT(合同)、INVOICE(发票)、ORDER(订单)' ,
  `execute_timing` VARCHAR(200) NOT NULL   COMMENT '执行时机(JSON);执行时机：CREATE(创建)、EDIT(编辑)' ,
  `enable` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '启用状态;启用状态：0-禁用，1-启用' ,
  `submitter_can_revoke` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '允许提交人撤销;允许提交人撤销审批中的申请' ,
  `allow_batch_process` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许批量处理;允许审批人批量处理此流程的多个任务' ,
  `allow_withdraw` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许撤回;允许审批人撤回审批' ,
  `allow_add_sign` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '允许加签' ,
  `duplicate_approver_rule` VARCHAR(20) NOT NULL  DEFAULT 'FIRST_ONLY' COMMENT '重复审批人：FIRST_ONLY/SEQUENTIAL_ALL/EACH' ,
  `require_comment` TINYINT(1) NOT NULL  DEFAULT 0 COMMENT '是否必须填写审批意见' ,
  `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
  `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
  `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
  `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
  PRIMARY KEY (id)
)  COMMENT = '审批流表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE approval_flow_blob(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `status_permissions` VARCHAR(2000) NOT NULL   COMMENT '状态权限配置（JSON格式）' ,
   `description` VARCHAR(3000)    COMMENT '流程描述' ,
   PRIMARY KEY (id)
)  COMMENT = '审批流大字段表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE approval_node(
  `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
  `flow_id` VARCHAR(32) NOT NULL   COMMENT '流程ID' ,
  `name` VARCHAR(200) NOT NULL   COMMENT '节点名称' ,
  `node_type` VARCHAR(50) NOT NULL   COMMENT '节点类型：START\IF\ELSE\END' ,
  `sort` INT(11) NOT NULL  DEFAULT 0 COMMENT '排序序号' ,
  PRIMARY KEY (id)
)  COMMENT = '审批节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_id ON approval_node(flow_id ASC);

CREATE TABLE approval_node_approver(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `flow_id` VARCHAR(32) NOT NULL   COMMENT '流程ID;流程ID' ,
   `approval_type` VARCHAR(20) NOT NULL  DEFAULT 'AUTO_PASS' COMMENT '审批类型;审批类型：MANUAL(人工审批)、AUTO_PASS(自动通过)、AUTO_REJECT(自动拒绝)' ,
   `multi_approver_mode` VARCHAR(20) NOT NULL   COMMENT '多人审批方式;多人审批方式：ALL(会签)/ANY(或签)/SEQUENTIAL(依次审批)' ,
   `empty_approver_action` VARCHAR(20) NOT NULL  DEFAULT 'AUTO_PASS' COMMENT '审批人为空时动作：AUTO_PASS(自动通过)/ASSIGN_SPECIFIC(指定人员审批)/ASSIGN_ADMIN(转交给审批管理员)' ,
   `same_submitter_action` VARCHAR(20) NOT NULL  DEFAULT 'SKIP' COMMENT '审批人与提交人相同时动作：SKIP(自动跳过)/ALLOW(由提交人审批)/ASSIGN_SUPERIOR(转交给直属上级审批)' ,
   `cc` VARCHAR(2000)    COMMENT '抄送人（JSON数组）' ,
   `approver` VARCHAR(2000)    COMMENT '审批人（JSON数组）' ,
   `pass_update_config` VARCHAR(2000)    COMMENT '审批通过后配置（JSON格式）' ,
   `reject_update_config` VARCHAR(2000)    COMMENT '审批驳回后配置（JSON格式）' ,
   `field_permissions` VARCHAR(2000)    COMMENT '字段权限配置（JSON格式）' ,
   PRIMARY KEY (id)
)  COMMENT = '审批人节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_id ON approval_node_approver(flow_id ASC);

CREATE TABLE approval_node_condition(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `flow_id` VARCHAR(32) NOT NULL   COMMENT '流程ID' ,
    `rule_expression` VARCHAR(2000) NOT NULL   COMMENT '条件表达式JSON数组' ,
    PRIMARY KEY (id,flow_id)
)  COMMENT = '条件节点配置表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_id ON approval_node_condition(flow_id ASC);

CREATE TABLE approval_node_link(
   `id` VARCHAR(32) NOT NULL   COMMENT '主键' ,
   `flow_id` VARCHAR(32) NOT NULL   COMMENT '流程ID' ,
   `from_node_id` VARCHAR(32) NOT NULL   COMMENT '源节点ID' ,
   `to_node_id` VARCHAR(32) NOT NULL   COMMENT '目标节点ID' ,
   `sort` INT NOT NULL  DEFAULT 0 COMMENT '分支评估顺序' ,
   PRIMARY KEY (id)
)  COMMENT = '节点连接表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_flow_id_from_id ON approval_node_link(flow_id ASC,from_node_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;