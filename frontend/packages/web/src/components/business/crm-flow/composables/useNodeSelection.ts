import { computed, type Ref, ref } from 'vue';

import { findBranchById, findNodeById } from '../dsl/queries';
import type { FlowSchema, NodeSelectionState } from '../types';

export default function useNodeSelection(flow: Ref<FlowSchema>) {
  type SelectionState = { type: 'none' } | { type: 'node'; id: string } | { type: 'branch'; id: string };
  const selectionState = ref<SelectionState>({ type: 'none' });

  const selection = computed<NodeSelectionState>(() => {
    if (selectionState.value.type === 'none') {
      return { type: 'none' };
    }

    if (selectionState.value.type === 'node') {
      const node = findNodeById(flow.value.nodes, selectionState.value.id);
      if (!node) {
        return { type: 'none' };
      }

      return {
        type: 'node',
        id: selectionState.value.id,
        node,
      };
    }

    const branch = findBranchById(flow.value.nodes, selectionState.value.id);
    if (!branch) {
      return { type: 'none' };
    }

    return {
      type: 'branch',
      id: selectionState.value.id,
      branch,
    };
  });

  function selectNode(nodeId: string) {
    selectionState.value = {
      type: 'node',
      id: nodeId,
    };
  }

  function selectBranch(branchId: string) {
    selectionState.value = {
      type: 'branch',
      id: branchId,
    };
  }

  function clearSelection() {
    selectionState.value = {
      type: 'none',
    };
  }

  return {
    selection,
    selectNode,
    selectBranch,
    clearSelection,
  };
}
