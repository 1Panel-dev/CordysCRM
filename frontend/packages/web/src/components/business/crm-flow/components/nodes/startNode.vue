<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="start"
    :icon="{
      type: 'iconicon_play_circle_stroke',
      backgroundColor: 'var(--primary-8)',
    }"
  />
</template>

<script setup lang="ts">
  import { toRef } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import useX6NodeData from '../../composables/useX6NodeData';
  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'StartNode',
  });

  const props = defineProps<{
    node?: Node;
  }>();

  const { nodeData } = useX6NodeData<{
    name?: string;
    description?: string;
    showContent?: boolean;
    selected?: boolean;
  }>(toRef(props, 'node'));
</script>
