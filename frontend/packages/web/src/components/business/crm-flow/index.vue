<template>
  <div class="crm-flow relative flex h-full w-full">
    <div class="crm-flow__main relative flex-1 overflow-hidden">
      <FlowCanvas
        :flow="flow"
        :selection="selection"
        @node-click="handleNodeClick"
        @branch-click="handleBranchClick"
        @node-delete="handleNodeDelete"
        @branch-delete="handleBranchDelete"
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
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import FlowCanvas from './components/canvas/flowCanvas.vue';

  import useModal from '@/hooks/useModal';

  import useNodeSelection from './composables/useNodeSelection';
  import { deleteConditionBranch, deleteNodeById } from './dsl/actions';
  import type { BranchClickPayload, NodeClickPayload } from './graph/types';
  import type { FlowSchema } from './types';

  const emit = defineEmits<{
    (event: 'addConditionBranch', groupId: string): void;
  }>();

  const model = defineModel<FlowSchema>('model', {
    required: true,
  });

  const slots = useSlots();
  const hasInsertNodeContentSlot = computed(() => Boolean(slots.insertNodeContent));
  const hasRightContentSlot = computed(() => Boolean(slots.rightContent));

  const flow = ref<FlowSchema>(model.value);
  const { selection, selectNode, selectBranch, clearSelection } = useNodeSelection(flow);
  const { t } = useI18n();
  const { openModal } = useModal();

  const syncingFromProps = ref(false);

  watch(
    model,
    (value) => {
      if (value) {
        syncingFromProps.value = true;
        flow.value = cloneDeep(value);
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
    emit('addConditionBranch', groupId);
  }

  function handleNodeDelete(payload: { nodeId: string }) {
    deleteNodeById(flow.value, payload.nodeId);
    clearSelection();
  }

  function handleBranchDelete(payload: { groupId: string; branchId: string }) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirm'),
      content: t('crmFlow.deleteConditionBranchConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          deleteConditionBranch(flow.value, payload.groupId, payload.branchId);
          clearSelection();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
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
