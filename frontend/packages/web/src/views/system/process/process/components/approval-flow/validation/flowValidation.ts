import { nextTick, watch } from 'vue';

import { ApprovalTypeEnum, ApproverTypeEnum, EmptyApproverActionEnum } from '@lib/shared/enums/process';
import type { ApprovalActionNode, ApprovalConditionBranch, BasicFormParams } from '@lib/shared/models/system/process';

import type { FlowNode, FlowSchema } from '@/components/business/crm-flow/types';

interface FlowValidationResult {
  invalidNodeIds: string[];
}

// 流程图不是单链表，条件组下面还会递归挂子节点，所以统一用一个 DFS 遍历入口。
function walkFlowNodes(
  nodes: FlowNode[],
  visitor: (payload: { node?: FlowNode; branch?: ApprovalConditionBranch }) => void
) {
  nodes.forEach((node) => {
    visitor({ node });

    if (node.type !== 'condition-group') {
      return;
    }

    (node.branches as ApprovalConditionBranch[]).forEach((branch) => {
      visitor({ branch });
      walkFlowNodes(branch.children, visitor);
    });
  });
}

function findStartNode(nodes: FlowNode[]) {
  return nodes.find((node) => node.type === 'start');
}

function isEmptyValue(value: unknown) {
  return value === null || value === undefined || (typeof value === 'string' && !value.trim());
}

function isMemberOrRole(type?: ApproverTypeEnum | null) {
  return !!type && [ApproverTypeEnum.SPECIFIED_MEMBER, ApproverTypeEnum.ROLE].includes(type);
}

// 只挑“开始节点校验相关”的字段做监听，避免无关字段变化时也触发清错。
function createBasicConfigValidationSnapshot(basicConfig: BasicFormParams) {
  return {
    name: basicConfig.name,
    formType: basicConfig.formType,
    createExecute: basicConfig.createExecute,
    updateExecute: basicConfig.updateExecute,
  };
}

export function clearInvalidState(target?: { invalid?: boolean } | null) {
  if (!target) {
    return;
  }

  target.invalid = false;
}

// 保存前先全量清空 invalid，再按照本次校验结果重新打标，避免旧红框残留。
function clearFlowInvalidMarks(flowSchema: FlowSchema) {
  walkFlowNodes(flowSchema.nodes, ({ node, branch }) => {
    clearInvalidState(node);
    clearInvalidState(branch);
  });
}

// 当前业务只有开始节点和审批节点会参与红框校验，所以这里只回写 node.invalid。
function applyFlowInvalidMarks(flowSchema: FlowSchema, result: FlowValidationResult) {
  const invalidNodeIds = new Set(result.invalidNodeIds);

  walkFlowNodes(flowSchema.nodes, ({ node }) => {
    if (node && invalidNodeIds.has(node.id)) {
      node.invalid = true;
    }
  });
}

function clearStartNodeInvalid(flowSchema: FlowSchema) {
  clearInvalidState(findStartNode(flowSchema.nodes));
}

// 开始节点的红框来自流程基础配置，而不是节点自身表单数据，所以单独映射到 start 节点。
function validateBasicConfig(flowSchema: FlowSchema, basicConfig: BasicFormParams, result: FlowValidationResult) {
  const startNode = findStartNode(flowSchema.nodes);
  if (
    startNode &&
    (isEmptyValue(basicConfig.name) ||
      isEmptyValue(basicConfig.formType) ||
      (!basicConfig.createExecute && !basicConfig.updateExecute))
  ) {
    result.invalidNodeIds.push(startNode.id);
  }
}

// 审批节点的规则全部走纯数据校验，不依赖右侧面板有没有渲染。
function validateApprovalActionNode(node: ApprovalActionNode, result: FlowValidationResult) {
  const isManualApproval = node.approvalType === ApprovalTypeEnum.MANUAL;
  const approverList = node.approverList ?? [];
  const ccList = node.ccList ?? [];
  const isInvalid =
    isEmptyValue(node.name) ||
    (isManualApproval &&
      (isEmptyValue(node.approverType) ||
        (isMemberOrRole(node.approverType) && approverList.length === 0) ||
        (node.emptyApproverAction === EmptyApproverActionEnum.ASSIGN_SPECIFIC && isEmptyValue(node.fallbackApprover)) ||
        (isMemberOrRole(node.ccType) && ccList.length === 0)));

  if (isInvalid) {
    result.invalidNodeIds.push(node.id);
  }
}

// 保存前的总入口：收集所有需要标红的节点 id，但不直接处理 UI 选中态。
function validateFlow(flowSchema: FlowSchema, basicConfig: BasicFormParams): FlowValidationResult {
  const result: FlowValidationResult = {
    invalidNodeIds: [],
  };

  validateBasicConfig(flowSchema, basicConfig, result);
  walkFlowNodes(flowSchema.nodes, ({ node }) => {
    if (node?.type === 'action' && node.actionType === 'approval') {
      validateApprovalActionNode(node as ApprovalActionNode, result);
    }
  });

  return result;
}

export default function useFlowValidation(params: {
  flowSchema: Ref<FlowSchema>;
  basicConfig: Ref<BasicFormParams>;
  selectNode?: (id: string) => void;
}) {
  // 校验失败时只跳到第一个错误节点，交互上更明确，也避免来回切换选中态。
  function selectInvalidFlowItem(invalidNodeIds: string[]) {
    const firstNodeId = invalidNodeIds[0];
    if (firstNodeId) {
      params.selectNode?.(firstNodeId);
    }
  }

  // 给保存按钮调用：一次完成“清旧状态 -> 全量校验 -> 回写红框 -> 定位首个错误节点”。
  function validateFlowNodes() {
    clearFlowInvalidMarks(params.flowSchema.value);
    const result = validateFlow(params.flowSchema.value, params.basicConfig.value);
    applyFlowInvalidMarks(params.flowSchema.value, result);

    if (result.invalidNodeIds.length) {
      nextTick(() => {
        selectInvalidFlowItem(result.invalidNodeIds);
      });
      return false;
    }

    return true;
  }

  // 开始节点基础配置一旦被修改，就先把开始节点红框去掉，体验上比一直红着更自然。
  watch(
    () => createBasicConfigValidationSnapshot(params.basicConfig.value),
    () => {
      clearStartNodeInvalid(params.flowSchema.value);
    },
    {
      deep: true,
    }
  );

  return {
    validateFlowNodes,
  };
}
