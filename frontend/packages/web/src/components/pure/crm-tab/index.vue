<template>
  <n-tabs v-model:value="activeTab" :type="props.type" :size="props.size" @update:value="handleChange">
    <n-tab-pane v-for="item of props.tabList" :key="item.name" :name="item.name as string" :tab="item.tab">
    </n-tab-pane>
  </n-tabs>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs, TabPaneProps } from 'naive-ui';

  const props = withDefaults(
    defineProps<{
      tabList: TabPaneProps[];
      type?: 'bar' | 'line' | 'card' | 'segment';
      size?: 'small' | 'medium' | 'large';
    }>(),
    {
      size: 'medium',
    }
  );

  const activeTab = defineModel<string | number>('activeTab', {
    default: '',
  });

  const emit = defineEmits<{
    (e: 'change', value: string | number): void;
  }>();

  function handleChange(value: string | number) {
    emit('change', value);
  }
</script>
