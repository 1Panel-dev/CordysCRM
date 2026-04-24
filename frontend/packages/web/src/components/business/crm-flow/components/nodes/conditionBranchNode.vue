<template>
  <!-- TODO lmy edit-permission -->
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="condition-branch"
    :deletable="!displayIsElse"
    title-editable
    :edit-permission="['']"
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
    description?: string;
    showContent?: boolean;
    isElse?: boolean;
    selected?: boolean;
  }>(toRef(props, 'node'));

  const displayIsElse = computed(() => nodeData.value.isElse ?? false);

  function handleDelete() {
    emit('delete');
  }

  function handleTitleEdit(value: string, done?: () => void) {
    const data = nodeData.value;
    renameFlowByGraphData(data, value);
    props.node?.setData?.({
      ...data,
      name: value,
    });
    done?.();
  }
</script>
