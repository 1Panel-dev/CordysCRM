import { computed, type Ref, ref } from 'vue';

import { findBranchById, findNodeById } from '../dsl/queries';
import type { ConditionBranch, FlowNode, FlowSchema, NodeSelectionState, SelectionType } from '../types';

export default function useNodeSelection(flow: Ref<FlowSchema>) {
  const selectionType = ref<SelectionType>('none');
  const selectedNodeId = ref<string | null>(null);
  const selectedBranchId = ref<string | null>(null);

  const selectedNode = computed<FlowNode | null>(() => {
    if (!selectedNodeId.value) {
      return null;
    }

    return findNodeById(flow.value.nodes, selectedNodeId.value);
  });

  const selectedBranch = computed<ConditionBranch | null>(() => {
    if (!selectedBranchId.value) {
      return null;
    }

    return findBranchById(flow.value.nodes, selectedBranchId.value);
  });

  const selection = computed<NodeSelectionState>(() => ({
    selectionType: selectionType.value,
    selectedNode: selectedNode.value,
    selectedBranch: selectedBranch.value,
  }));

  function selectNode(nodeId: string) {
    selectionType.value = 'node';
    selectedNodeId.value = nodeId;
    selectedBranchId.value = null;
  }

  function selectBranch(branchId: string) {
    selectionType.value = 'branch';
    selectedBranchId.value = branchId;
    selectedNodeId.value = null;
  }

  function clearSelection() {
    selectionType.value = 'none';
    selectedNodeId.value = null;
    selectedBranchId.value = null;
  }

  return {
    selection,
    selectNode,
    selectBranch,
    clearSelection,
  };
}
