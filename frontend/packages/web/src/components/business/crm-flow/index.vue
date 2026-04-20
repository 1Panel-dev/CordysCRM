<template>
  <div class="crm-flow relative flex h-full w-full">
    <div class="crm-flow__main relative flex-1 overflow-hidden">
      <FlowCanvas
        :flow="flow"
        @node-click="handleNodeClick"
        @branch-click="handleBranchClick"
        @add-condition-branch="handleAddConditionBranch"
        @blank-click="clearSelection"
      >
        <template v-if="hasInsertNodeContentSlot" #insertNodeContent="{ anchorNodeId, anchorBranch }">
          <slot name="insertNodeContent" :anchorNodeId="anchorNodeId" :anchorBranch="anchorBranch" />
        </template>
      </FlowCanvas>
    </div>

    <div v-if="hasRightContentSlot" class="crm-flow__sidebar">
      <slot name="rightContent" :selection="selection" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, useSlots, watch } from 'vue';

  import FlowCanvas from './components/canvas/flowCanvas.vue';

  import useFlowDesigner from './composables/useFlowDesigner';
  import useNodeSelection from './composables/useNodeSelection';
  import type { BranchClickPayload, NodeClickPayload } from './graph/types';
  import type { FlowSchema } from './types';

  const model = defineModel<FlowSchema>('model', {
    required: true,
  });

  const slots = useSlots();
  const hasInsertNodeContentSlot = computed(() => Boolean(slots.insertNodeContent));
  const hasRightContentSlot = computed(() => Boolean(slots.rightContent));

  const { flow, setFlow, addBranchToConditionGroup } = useFlowDesigner(model.value);
  const { selection, selectNode, selectBranch, clearSelection } = useNodeSelection(flow);

  const syncingFromProps = ref(false);

  watch(
    model,
    (value) => {
      if (value) {
        syncingFromProps.value = true;
        setFlow(value);
      }
    },
    {
      deep: true,
    }
  );

  watch(
    flow,
    (value) => {
      if (syncingFromProps.value) {
        syncingFromProps.value = false;
        return;
      }
      model.value = value;
    },
    {
      deep: true,
    }
  );

  function handleNodeClick(payload: NodeClickPayload) {
    selectNode(payload.nodeId);
  }

  function handleBranchClick(payload: BranchClickPayload) {
    selectBranch(payload.branchId);
  }

  function handleAddConditionBranch(groupId: string) {
    addBranchToConditionGroup(groupId);
  }

  defineExpose({
    flow,
  });
</script>

<style lang="less" scoped>
  .crm-flow {
    background: var(--text-n9);
  }
  .crm-flow__sidebar {
    overflow: auto;
    width: 400px;
    background: var(--text-n10);
  }
</style>
