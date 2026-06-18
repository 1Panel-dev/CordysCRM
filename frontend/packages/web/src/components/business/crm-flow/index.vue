<template>
  <div class="crm-flow relative flex h-full w-full">
    <div class="crm-flow__main relative flex-1 overflow-hidden">
      <!-- canvasFlow 只影响画布展示；右侧表单和保存仍然使用 model 原始数据。 -->
      <FlowCanvas
        ref="flowCanvasRef"
        :readonly="props.readonly"
        :flow="props.canvasFlow ?? flow"
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

    <div v-if="showRightContent" class="crm-flow__sidebar">
      <slot name="rightContent" :selection="selection" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref, useSlots, watch } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import FlowCanvas from './components/canvas/flowCanvas.vue';

  import useModal from '@/hooks/useModal';

  import useNodeSelection from './composables/useNodeSelection';
  import { deleteConditionBranch, deleteNodeById } from './dsl/actions';
  import type { BranchClickPayload, NodeClickPayload } from './graph/types';
  import type { FlowSchema, NodeSelectionState } from './types';

  const emit = defineEmits<{
    (event: 'addConditionBranch', groupId: string): void;
    (event: 'branchClick', payload: BranchClickPayload): void;
  }>();

  const props = defineProps<{
    canvasFlow?: FlowSchema;
    rightContentVisible?: (selection: NodeSelectionState) => boolean;
    readonly?: boolean;
  }>();

  const flow = defineModel<FlowSchema>('model', {
    required: true,
  });

  const slots = useSlots();
  const hasInsertNodeContentSlot = computed(() => Boolean(slots.insertNodeContent));
  const hasRightContentSlot = computed(() => Boolean(slots.rightContent));

  const { selection, selectNode, selectBranch, clearSelection } = useNodeSelection(flow);
  const { t } = useI18n();
  const { openModal } = useModal();

  const showRightContent = computed(() => {
    if (!hasRightContentSlot.value) {
      return false;
    }

    return props.rightContentVisible?.(selection.value);
  });

  function handleNodeClick(payload: NodeClickPayload) {
    selectNode(payload.nodeId);
  }

  function handleBranchClick(payload: BranchClickPayload) {
    selectBranch(payload.branchId);
    emit('branchClick', payload);
  }

  function handleAddConditionBranch(groupId: string) {
    if (props.readonly) {
      return;
    }
    emit('addConditionBranch', groupId);
  }

  function handleNodeDelete(payload: { nodeId: string }) {
    if (props.readonly) {
      return;
    }
    deleteNodeById(flow.value, payload.nodeId);
    clearSelection();
  }

  function handleBranchDelete(payload: { groupId: string; branchId: string }) {
    if (props.readonly) {
      return;
    }
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

  const flowCanvasRef = ref<InstanceType<typeof FlowCanvas> | null>(null);

  function refreshCanvas(fitToContent = false) {
    flowCanvasRef.value?.refreshCanvas(fitToContent);
  }

  watch(showRightContent, () => {
    nextTick(() => {
      refreshCanvas(true);
    });
  });

  defineExpose({
    flow,
    selectNode,
    refreshCanvas,
  });
</script>

<style lang="less" scoped>
  .crm-flow {
    background: var(--text-n9);
  }
  .crm-flow__sidebar {
    overflow: hidden;
    width: 400px;
    background: var(--text-n10);
  }
</style>
