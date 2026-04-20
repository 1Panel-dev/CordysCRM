import { ref } from 'vue';
import { cloneDeep } from 'lodash-es';

import {
  addConditionBranch,
  deleteConditionBranch,
  deleteNodeById,
  insertNodeAfterNode,
  insertNodeToConditionBranch,
  updateNodeById,
} from '../dsl/actions';
import { createActionNode, createConditionGroupNode } from '../dsl/factory';
import type { ActionNode, ConditionGroupNode, FlowNode, FlowSchema } from '../types';

export default function useFlowDesigner(initialFlow: FlowSchema) {
  const flow = ref<FlowSchema>(initialFlow);

  function setFlow(nextFlow: FlowSchema) {
    flow.value = cloneDeep(nextFlow);
  }

  // 增加活动分支
  function addActionAfter(anchorNodeId: string, partial?: Partial<ActionNode>) {
    const newNode = createActionNode(partial);
    insertNodeAfterNode(flow.value, anchorNodeId, newNode);
  }

  function addConditionGroupAfter(anchorNodeId: string, partial?: Partial<ConditionGroupNode>) {
    const newNode = createConditionGroupNode(partial);
    insertNodeAfterNode(flow.value, anchorNodeId, newNode);
  }

  // 分支内插入节点
  function addNodeToConditionBranch(groupId: string, branchId: string, type: 'action' | 'condition-group') {
    const newNode = type === 'action' ? createActionNode() : createConditionGroupNode();
    insertNodeToConditionBranch(flow.value, groupId, branchId, newNode);
  }

  function addBranchToConditionGroup(groupId: string) {
    addConditionBranch(flow.value, groupId);
  }

  function removeConditionBranch(groupId: string, branchId: string) {
    deleteConditionBranch(flow.value, groupId, branchId);
  }

  function removeNode(nodeId: string) {
    deleteNodeById(flow.value, nodeId);
  }

  function updateNode(nodeId: string, patch: Partial<FlowNode>) {
    updateNodeById(flow.value, nodeId, patch);
  }

  return {
    flow,
    setFlow,
    addActionAfter,
    addConditionGroupAfter,
    addNodeToConditionBranch,
    addBranchToConditionGroup,
    removeConditionBranch,
    removeNode,
    updateNode,
  };
}
