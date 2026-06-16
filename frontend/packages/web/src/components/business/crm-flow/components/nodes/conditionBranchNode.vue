<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :number="nodeData.number"
    :description="nodeData.description"
    :priority-label="priorityLabel"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    :invalid="Boolean(nodeData.invalid)"
    :titleEditable="Boolean(!displayIsElse && !nodeData.isPanMode && !nodeData.readonly)"
    node-type="condition-branch"
    :deletable="!displayIsElse && !nodeData.readonly"
    :icon="{
      type: 'iconicon_fork',
      backgroundColor: 'var(--info-blue)',
    }"
    @delete="handleDelete"
    @title-edit="handleTitleEdit"
  />
</template>

<script setup lang="ts">
  import { computed, toRef } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import useX6NodeData from '../../composables/useX6NodeData';
  import { renameFlowByGraphData } from '../../graph/renameRegistry';
  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'ConditionBranchNode',
  });

  const props = defineProps<{
    node?: Node;
  }>();

  const emit = defineEmits<{
    (event: 'delete'): void;
  }>();

  const { nodeData } = useX6NodeData<{
    kind: 'condition-branch';
    groupId?: string;
    branchId?: string;
    name?: string;
    number?: string;
    sort?: number;
    description?: string;
    showContent?: boolean;
    isElse?: boolean;
    selected?: boolean;
    invalid?: boolean;
    readonly?: boolean;
    isPanMode?: boolean;
  }>(toRef(props, 'node'));

  const displayIsElse = computed(() => nodeData.value.isElse ?? false);
  const priorityLabel = computed(() => (!displayIsElse.value ? `P${nodeData.value.sort ?? 1}` : ''));

  function handleDelete() {
    emit('delete');
  }

  function handleTitleEdit(value: string, done?: () => void) {
    if (displayIsElse.value) {
      return;
    }

    const data = nodeData.value;
    renameFlowByGraphData(data, value);
    props.node?.setData?.({
      ...data,
      name: value,
      invalid: false,
    });
    done?.();
  }
</script>
