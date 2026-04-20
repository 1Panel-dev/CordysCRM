<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="condition-branch"
    :deletable="!displayIsElse"
    :icon="{
      type: 'iconicon_fork',
      backgroundColor: 'var(--info-blue)',
    }"
    @delete="handleDelete"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

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

  const nodeData = computed(
    () =>
      (props.node?.getData?.() ?? {}) as {
        name?: string;
        description?: string;
        showContent?: boolean;
        isElse?: boolean;
        selected?: boolean;
      }
  );
  const displayIsElse = computed(() => nodeData.value.isElse ?? false);

  function handleDelete() {
    emit('delete');
  }
</script>
